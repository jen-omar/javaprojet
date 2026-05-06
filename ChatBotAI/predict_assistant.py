#!/usr/bin/env python3
"""
Marketplace AI Assistant - Prediction Script (Client-Only, Bilingual FR/EN)
Called by ChatbotService.java: python predict_assistant.py <message> <context> [user_name]
Output: JSON with intent, confidence, response
"""
import json, math, os, re, sys, mysql.connector
from collections import Counter
from difflib import get_close_matches

# ── Typo maps (EN + FR) ────────────────────────────────────
TYPO_MAP = {
    'paintng':'painting','paiting':'painting','paintins':'paintings','sculture':'sculpture',
    'photgraphy':'photography','fotografy':'photography','digtal':'digital','drawin':'drawing',
    'wishlst':'wishlist','whishlist':'wishlist','favourites':'favorites','favs':'favorites',
    'ordrs':'orders','oders':'orders','chekout':'checkout','purchse':'purchase',
    'delivry':'delivery','shpping':'shipping','recomend':'recommend','srch':'search',
    'serch':'search','fnd':'find','shw':'show','hlp':'help','plz':'please','pls':'please',
    'thnks':'thanks','thx':'thanks','reveiw':'review','rting':'rating','aucton':'auction',
    'biding':'bidding','wanna':'want to','gonna':'going to','dunno':'do not know',
    'prdoct':'product','prodct':'product','pice':'price','pirce':'price',
    'ur':'your','u':'you','im':'i am','dont':'do not','cant':'can not',
    'whats':'what is','hows':'how is','info':'information',
    # French typos
    'peintur':'peinture','peintres':'peintures','scultur':'sculpture','enchèr':'enchère',
    'comande':'commande','comandes':'commandes','favori':'favoris','souhait':'souhaits',
    'produi':'produit','produis':'produits','nouvauté':'nouveauté','livraisn':'livraison',
    'paiment':'paiement','conection':'connexion','recomander':'recommander',
    'cherche':'chercher','montre':'montrer','affichr':'afficher',
}

def normalize_text(text):
    text = text.strip().lower()
    text = re.sub(r'[.]{2,}', ' ', text)
    text = re.sub(r'[!]{2,}', '!', text)
    text = re.sub(r'[?]{2,}', '?', text)
    text = re.sub(r'\b(uh+|um+|hmm+|ahh+|ohh+|euh+)\b', '', text)
    text = re.sub(r'(.)\1{2,}', r'\1\1', text)
    words = text.split()
    corrected = []
    for w in words:
        cw = re.sub(r"[^a-zàâäéèêëïîôùûüÿçœæ'\-]", '', w)
        corrected.append(TYPO_MAP.get(cw, w))
    return re.sub(r'\s+', ' ', ' '.join(corrected)).strip()

# ── Language detection ──────────────────────────────────────
FR_WORDS = {'je','tu','il','elle','nous','vous','ils','elles','le','la','les','un','une','des',
    'mon','ma','mes','ton','ta','tes','son','sa','ses','ce','cette','ces','et','ou','mais',
    'donc','car','que','qui','quoi','comment','pourquoi','où','quand','est','sont','suis',
    'avoir','être','faire','voir','montrer','chercher','trouver','acheter','vendre',
    'bonjour','salut','merci','oui','non','bien','pas','cher','oeuvre','produit','prix',
    'enchère','commande','peinture','sculpture','dessin','livraison','paiement','avis',
    'aide','favoris','souhaits','panier','nouveau','disponible','artiste','catalogue'}

def detect_lang(text):
    words = set(re.findall(r'[a-zàâäéèêëïîôùûüÿçœæ]+', text.lower()))
    fr_count = len(words & FR_WORDS)
    return 'fr' if fr_count >= 2 or any(c in text for c in 'àâéèêëïîôùûüÿçœæ') else 'en'

# ── Entity extraction ───────────────────────────────────────
def extract_entities(message):
    entities = {}
    msg = message.lower()
    # Price
    for pat, etype in [(r'(?:under|sous|moins de)\s*\$?(\d+)','max_price'),
                       (r'(?:over|plus de|above)\s*\$?(\d+)','min_price'),
                       (r'\$?(\d+)\s*(?:to|-|à)\s*\$?(\d+)','price_range')]:
        m = re.search(pat, msg)
        if m:
            if etype == 'price_range':
                entities['min_price'] = float(m.group(1))
                entities['max_price'] = float(m.group(2))
            else:
                entities[etype] = float(m.group(1))
            break
    # Type
    for kw, t in {'painting':'Painting','peinture':'Painting','tableau':'Painting',
                  'sculpture':'Sculpture','digital':'Digital Art','numérique':'Digital Art',
                  'photo':'Photography','dessin':'Drawing','drawing':'Drawing',
                  'mixed':'Mixed Media'}.items():
        if kw in msg:
            entities['type'] = t; break
    # Category
    for kw, c in {'abstract':'Abstract','abstrait':'Abstract','landscape':'Landscape',
                  'paysage':'Landscape','portrait':'Portrait','still life':'Still Life',
                  'nature morte':'Still Life','modern':'Modern','moderne':'Modern',
                  'classical':'Classical','classique':'Classical','impressionist':'Impressionist',
                  'impressionniste':'Impressionist','pop art':'Pop Art',
                  'minimalist':'Minimalist','minimaliste':'Minimalist',
                  'surrealist':'Surrealist','surréaliste':'Surrealist'}.items():
        if kw in msg:
            entities['category'] = c; break
    # Artist name
    am = re.search(r'(?:by|de|from|par)\s+([A-Za-zÀ-ÿ][a-zA-ZÀ-ÿ\s]+)', message)
    if am:
        entities['artist_name'] = am.group(1).strip()
    return entities

# ── Keyword intent matching (FR+EN) ────────────────────────
INTENT_KEYWORDS = {
    'greeting':['hello','hi','hey','bonjour','salut','coucou','bonsoir'],
    'help':['help','aide','menu','guide','commandes','commands'],
    'thanks':['thanks','thank','merci','appreciate'],
    'goodbye':['bye','goodbye','revoir','plus','ciao','bonne nuit'],
    'search_products':['browse','products','produits','catalogue','collection','explorer'],
    'search_paintings':['painting','paintings','peinture','peintures','tableau','tableaux','toile'],
    'search_sculptures':['sculpture','sculptures','statue','statues'],
    'search_digital':['digital','nft','pixel','numérique'],
    'search_photography':['photography','photo','photos','photographie','photographies'],
    'search_drawings':['drawing','drawings','dessin','dessins','croquis','sketch'],
    'search_newest':['newest','latest','new','nouveau','nouveautés','récent','quoi de neuf'],
    'search_by_price_cheap':['cheap','affordable','budget','pas cher','abordable','économique','petit prix'],
    'search_by_price_expensive':['expensive','luxury','premium','cher','luxe','haut de gamme'],
    'search_by_artist':['artist','artiste'],
    'search_by_category':['abstract','landscape','portrait','abstrait','paysage','moderne','classique'],
    'view_auctions':['auction','auctions','enchère','enchères','bid on'],
    'bidding_help':['bidding','how to bid','enchérir','comment enchérir'],
    'order_history':['orders','order','commandes','commande','achats','purchases','acheté'],
    'cart_help':['cart','checkout','panier','acheter','caisse'],
    'wishlist_help':['wishlist','favoris','souhaits','sauvegarder'],
    'wishlist_view':['my wishlist','mes favoris','mes souhaits','show wishlist'],
    'cart_view':['my cart','mon panier','show cart','voir panier'],
    'recommend':['recommend','suggest','suggestion','recommander','conseille','surprise','tendance'],
    'product_detail':['details','detail','détails','information','décrire','describe'],
    'track_order':['track','tracking','suivre','suivi','where is my order','où est ma commande'],
    'compare_prices':['compare','comparison','comparer','comparaison','versus','vs'],
    'budget_recommend':['budget','value','rapport qualité','bon plan'],
    'review_guide':['review','avis','noter','rate','feedback','évaluer'],
    'shipping_info':['shipping','delivery','livraison','expédition','emballage'],
    'payment_help':['payment','paiement','stripe','carte','card','refund','remboursement'],
    'about_marketplace':['about','marketplace','platform','plateforme','à propos','site','c\'est quoi','quoi sert'],
    'account_help':['account','login','password','compte','connexion','mot de passe','profil'],
    'sell_art':['sell','vendre','devenir artiste','publier','poster','how to sell'],
    'creator_info':['who made','creator','développeur','créateur','fabriqué','équipe'],
    'small_talk':['how are you','comment vas-tu','ça va','ca va','qui es-tu','ton nom','what is your name'],
    'compliment':['great','awesome','good bot','bravo','génial','super bot'],
    'negative':['wrong','bad','stupid','nul','faux','mauvais','useless','inutile'],
}

def keyword_match(msg, context):
    msg_lower = msg.lower().strip()
    # Exact matches first
    greetings = {'hi','hello','hey','bonjour','salut','coucou','bonsoir','yo','slt','bjr','cc'}
    if msg_lower in greetings or any(msg_lower.startswith(g+' ') for g in ['hi','hey','hello','salut','bonjour']):
        return 'greeting'
    if msg_lower in {'help','help me','aide','aide moi','menu','les commandes'}:
        return 'help'
    if msg_lower in {'thanks','thank you','thx','merci','merci beaucoup','mrc'}:
        return 'thanks'
    if msg_lower in {'bye','goodbye','au revoir','à plus','ciao','bonne nuit','bye bye'}:
        return 'goodbye'

    # Search triggers
    triggers_en = ['show me','find me','search for','look for','i want','show','find','browse','get me']
    triggers_fr = ['montrer','montre moi','voir','afficher','chercher','je cherche','trouver','je veux']
    is_search = any(msg_lower.startswith(t) for t in triggers_en + triggers_fr)

    if is_search:
        if any(w in msg_lower for w in ['painting','peinture','tableau','toile']): return 'search_paintings'
        if any(w in msg_lower for w in ['sculpture','statue']): return 'search_sculptures'
        if any(w in msg_lower for w in ['digital','numérique']): return 'search_digital'
        if any(w in msg_lower for w in ['photo','photographie']): return 'search_photography'
        if any(w in msg_lower for w in ['drawing','dessin','croquis']): return 'search_drawings'
        return 'search_products'

    # Specific intent keywords
    checks = [
        (['auction','enchère','enchères','encheres'],'view_auctions'),
        (['how to bid','comment enchérir','enchérir','bidding help','aide enchère'],'bidding_help'),
        (['my order','mes commandes','order history','historique','what did i buy',"j'ai acheté"],'order_history'),
        (['track','suivre','suivi','where is my order','où est ma commande'],'track_order'),
        (['recommend','suggest','recommand','conseille','surprise','tendance'],'recommend'),
        (['newest','latest','new arrivals','nouveauté','quoi de neuf','nouveau','récent'],'search_newest'),
        (['wishlist','favoris','souhaits','mes favoris','my wishlist'],'wishlist_view'),
        (['cart','panier','mon panier','my cart'],'cart_view'),
        (['checkout','caisse','comment acheter','how to buy','how to checkout'],'cart_help'),
        (['cheap','affordable','pas cher','abordable','petit prix','budget'],'search_by_price_cheap'),
        (['expensive','luxury','cher','luxe','premium'],'search_by_price_expensive'),
        (['compare','comparer','versus','vs','comparaison'],'compare_prices'),
        (['review','avis','noter','rate','évaluer','feedback'],'review_guide'),
        (['shipping','livraison','delivery','expédition'],'shipping_info'),
        (['payment','paiement','stripe','carte bancaire','how to pay','comment payer'],'payment_help'),
        (['about','à propos','c\'est quoi','what is this','marketplace info'],'about_marketplace'),
        (['account','compte','login','connexion','mot de passe','password','profil'],'account_help'),
        (['abstract','abstrait'],'search_by_category'),
        (['landscape','paysage'],'search_by_category'),
        (['portrait'],'search_by_category'),
        (['modern','moderne'],'search_by_category'),
        (['vendre','sell','comment vendre','publier','poster'],'sell_art'),
        (['qui es-tu','ton nom','nom','who are you','your name'],'small_talk'),
        (['créateur','développeur','qui a fait','made by','creator'],'creator_info'),
    ]
    for keywords, intent in checks:
        if any(w in msg_lower for w in keywords):
            if intent == 'view_auctions' and any(w in msg_lower for w in ['how','comment','explain','aide']):
                return 'bidding_help'
            return intent
    return None

# ── Fuzzy matching ──────────────────────────────────────────
def fuzzy_match_intent(text, threshold=0.75):
    words = re.findall(r'[a-zàâäéèêëïîôùûüÿçœæ]+', text.lower())
    all_kw = list(set(kw for kws in INTENT_KEYWORDS.values() for kw in kws))
    scores = {}
    for w in words:
        if len(w) < 3: continue
        for m in get_close_matches(w, all_kw, n=2, cutoff=threshold):
            for intent, kws in INTENT_KEYWORDS.items():
                if m in kws:
                    scores[intent] = scores.get(intent, 0) + 1
    return max(scores, key=scores.get) if scores else None

# ── TF-IDF functions ───────────────────────────────────────
def tokenize(text):
    return re.findall(r'[a-zàâäéèêëïîôùûüÿçœæ]+', text.lower())

def compute_tf(tokens):
    tf = Counter(tokens)
    total = len(tokens) if tokens else 1
    return {w: c/total for w, c in tf.items()}

def compute_tfidf_vector(text, idf):
    tokens = tokenize(text)
    tf = compute_tf(tokens)
    return {w: tf.get(w,0)*idf[w] for w in set(tokens) if w in idf}

def cosine_similarity(v1, v2):
    common = set(v1) & set(v2)
    if not common: return 0.0
    dot = sum(v1[k]*v2[k] for k in common)
    n1 = math.sqrt(sum(v*v for v in v1.values()))
    n2 = math.sqrt(sum(v*v for v in v2.values()))
    return dot/(n1*n2) if n1 and n2 else 0.0

def load_model(context):
    path = os.path.join(os.path.dirname(__file__), 'models', f'{context}_model.json')
    if not os.path.exists(path): return None
    with open(path, 'r', encoding='utf-8') as f: return json.load(f)

def load_responses():
    path = os.path.join(os.path.dirname(__file__), 'models', 'responses.json')
    if not os.path.exists(path): return {}
    with open(path, 'r', encoding='utf-8') as f: return json.load(f)

def predict_intent(model, message):
    vec = compute_tfidf_vector(message, model['idf'])
    best_intent, best_score = None, -1
    for intent, centroid in model['centroids'].items():
        score = cosine_similarity(vec, centroid)
        if score > best_score:
            best_score = score
            best_intent = intent
    return best_intent or 'help', min(best_score, 1.0) if best_score > 0 else 0.0

# ── DB dynamic responses ───────────────────────────────────
def get_db():
    return mysql.connector.connect(host="localhost", user="root", password="", database="marketplace")

def query_products(where_clause, params=None, lang='fr'):
    try:
        conn = get_db()
        cur = conn.cursor(dictionary=True)
        cur.execute(f"SELECT name, price, type, artist_name FROM product WHERE status != 'sold' AND {where_clause} LIMIT 5", params or ())
        rows = cur.fetchall()
        conn.close()
        return rows
    except:
        return []

def format_products(rows, title_fr, title_en, lang):
    if not rows:
        return "Aucun produit trouvé."
    title = title_fr if lang == 'fr' else title_en
    lines = [f"- {r['name']} : {r['price']} EUR (Artiste : {r['artist_name']})" for r in rows]
    return title + "\n" + "\n".join(lines)

def handle_dynamic(intent, entities, user_name, lang):
    try:
        if intent == 'search_products':
            rows = query_products("1=1 ORDER BY id DESC")
            return format_products(rows, "Produits disponibles :", "Available products:", lang)

        elif intent == 'search_paintings':
            rows = query_products("type = 'Painting'")
            return format_products(rows, "Peintures disponibles :", "Available paintings:", lang)

        elif intent == 'search_sculptures':
            rows = query_products("type = 'Sculpture'")
            return format_products(rows, "Sculptures disponibles :", "Available sculptures:", lang)

        elif intent == 'search_digital':
            rows = query_products("type = 'Digital Art'")
            return format_products(rows, "Art numérique disponible :", "Available digital art:", lang)

        elif intent == 'search_photography':
            rows = query_products("type = 'Photography'")
            return format_products(rows, "Photographies disponibles :", "Available photography:", lang)

        elif intent == 'search_drawings':
            rows = query_products("type = 'Drawing'")
            return format_products(rows, "Dessins disponibles :", "Available drawings:", lang)

        elif intent == 'search_newest':
            rows = query_products("1=1 ORDER BY id DESC")
            return format_products(rows, "Dernières nouveautés :", "Latest additions:", lang)

        elif intent == 'search_by_price_cheap':
            max_p = entities.get('max_price', 500)
            rows = query_products("price <= %s ORDER BY price ASC", (max_p,))
            return format_products(rows, f"Art à moins de {max_p} EUR :", f"Art under {max_p} EUR:", lang)

        elif intent == 'search_by_price_expensive':
            min_p = entities.get('min_price', 1000)
            rows = query_products("price >= %s ORDER BY price DESC", (min_p,))
            return format_products(rows, f"Art premium (plus de {min_p} EUR) :", f"Premium art (over {min_p} EUR):", lang)

        elif intent == 'search_by_artist':
            name = entities.get('artist_name', '')
            if name:
                rows = query_products("artist_name LIKE %s", (f'%{name}%',))
                return format_products(rows, f"Oeuvres de {name} :", f"Art by {name}:", lang)
            return "Veuillez préciser le nom de l'artiste."

        elif intent == 'search_by_category':
            cat = entities.get('category', '')
            if cat:
                rows = query_products("category = %s", (cat,))
                return format_products(rows, f"Art catégorie {cat} :", f"{cat} art:", lang)
            return "Catégories disponibles : Abstract, Landscape, Portrait, Still Life, Modern, Classical, Impressionist, Pop Art, Minimalist, Surrealist"

        elif intent == 'view_auctions':
            rows = query_products("sale_type = 'auction' ORDER BY auction_end_time ASC")
            if not rows:
                return "Aucune enchère en cours."
            lines = [f"- {r['name']} : {r['price']} EUR (Artiste : {r['artist_name']})" for r in rows]
            return "Enchères en cours :\n" + "\n".join(lines)

        elif intent == 'order_history':
            try:
                conn = get_db()
                cur = conn.cursor(dictionary=True)
                cur.execute("SELECT p.name, o.price, o.order_type, o.created_at FROM `order` o JOIN product p ON o.product_id = p.id WHERE o.buyer_name = %s ORDER BY o.id DESC LIMIT 5", (user_name,))
                rows = cur.fetchall()
                conn.close()
                if rows:
                    lines = [f"- {r['name']} : {r['price']} EUR ({r['order_type']})" for r in rows]
                    return "Vos dernières commandes :\n" + "\n".join(lines)
                return "Vous n'avez pas encore de commandes."
            except:
                return "Consultez l'onglet Commandes."

        elif intent == 'recommend':
            rows = query_products("1=1 ORDER BY RAND()")
            return format_products(rows, "Nos recommandations :", "Our recommendations:", lang)

        elif intent == 'wishlist_view':
            try:
                conn = get_db()
                cur = conn.cursor(dictionary=True)
                cur.execute("SELECT p.name, p.price FROM wishlist w JOIN product p ON w.product_id = p.id WHERE w.client_name = %s LIMIT 5", (user_name,))
                rows = cur.fetchall()
                conn.close()
                if rows:
                    lines = [f"- {r['name']} : {r['price']} EUR" for r in rows]
                    return "Votre liste de souhaits :\n" + "\n".join(lines)
                return "Votre liste de souhaits est vide."
            except:
                return "Consultez 'LISTE DE SOUHAITS' dans le menu."

        elif intent == 'cart_view':
            return "Le marketplace utilise l'achat direct. Cliquez sur 'Acheter' sur un produit pour commander."

        elif intent == 'track_order':
            return "Pour suivre votre commande, consultez vos achats. L'artiste vous contactera pour la livraison."

        elif intent == 'compare_prices':
            try:
                conn = get_db()
                cur = conn.cursor(dictionary=True)
                cur.execute("SELECT name, price, type FROM product WHERE status != 'sold' ORDER BY price ASC LIMIT 3")
                cheap = cur.fetchall()
                cur.execute("SELECT name, price, type FROM product WHERE status != 'sold' ORDER BY price DESC LIMIT 3")
                exp = cur.fetchall()
                conn.close()
                lines = ["Les moins chers :"] + [f"  - {r['name']} : {r['price']} EUR" for r in cheap]
                lines += ["", "Les plus chers :"] + [f"  - {r['name']} : {r['price']} EUR" for r in exp]
                return "\n".join(lines)
            except:
                return "Parcourez le catalogue et utilisez le tri par prix."

        elif intent == 'budget_recommend':
            max_p = entities.get('max_price', 500)
            rows = query_products("price <= %s ORDER BY price DESC", (max_p,))
            return format_products(rows, f"Meilleurs choix sous {max_p} EUR :", f"Best picks under {max_p} EUR:", lang)

    except Exception as e:
        pass
    return None

# ── Main ───────────────────────────────────────────────────
def main():
    if len(sys.argv) < 3:
        print(json.dumps({"success": False, "error": "Usage: predict_assistant.py <message> <context> [user_name]"}))
        sys.exit(1)

    raw_message = sys.argv[1]
    context = sys.argv[2]
    user_name = sys.argv[3] if len(sys.argv) > 3 else "Guest"

    message = normalize_text(raw_message)
    lang = detect_lang(raw_message)

    model = load_model(context)
    if model is None:
        print(json.dumps({"success": False, "error": "Model not trained. Run train_assistant.py first."}))
        sys.exit(1)

    all_responses = load_responses()
    context_responses = all_responses.get(context, {})

    # Step 1: Keyword match
    intent = keyword_match(message, context)
    confidence = 0.99 if intent else 0.0

    # Step 2: Fuzzy match
    if not intent:
        intent = fuzzy_match_intent(message)
        confidence = 0.85 if intent else 0.0

    # Step 3: ML model
    if not intent:
        intent, confidence = predict_intent(model, message)
        if confidence < 0.35:
            fuzzy = fuzzy_match_intent(raw_message.lower(), 0.65)
            if fuzzy:
                intent, confidence = fuzzy, 0.70
            else:
                intent, confidence = 'help', 0.35

    entities = extract_entities(message)
    response_template = context_responses.get(intent, "")
    needs_dynamic = response_template == "__DYNAMIC__"

    result = {"success": True, "intent": intent, "confidence": round(confidence, 4),
              "entities": entities, "needs_dynamic": needs_dynamic, "context": context, "user_name": user_name}

    if needs_dynamic:
        dyn_resp = handle_dynamic(intent, entities, user_name, lang)
        if dyn_resp:
            result["response"] = dyn_resp
            result["needs_dynamic"] = False

    if not needs_dynamic and response_template and not result.get("response"):
        result["response"] = response_template.replace("{user_name}", user_name)

    if not result.get("response"):
        result["response"] = "Désolée, je ne peux pas vous répondre."

    print(json.dumps(result, ensure_ascii=True))

if __name__ == '__main__':
    main()
