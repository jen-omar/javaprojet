#!/usr/bin/env python3
"""
Marketplace AI Assistant - Training Script (Client-Only, Bilingual FR/EN)
TF-IDF + Cosine Similarity classifier. No external ML deps required.
"""

import json
import math
import os
import re
from collections import Counter, defaultdict
from training_data import TRAINING_DATA

# ============================================================
# BILINGUAL RESPONSE TEMPLATES
# ============================================================

RESPONSES = {
    "client": {
        "greeting": (
            "👋 Bonjour {user_name} ! Je suis votre assistant marketplace.\n"
            "Hello {user_name}! I'm your marketplace assistant.\n\n"
            "Je peux vous aider à :\n"
            "• 🔍 Rechercher des produits (peintures, sculptures, art numérique...)\n"
            "• 🏷️ Filtrer par type, catégorie, prix ou artiste\n"
            "• 🔨 Voir les enchères et apprendre à enchérir\n"
            "• 📦 Consulter vos commandes\n"
            "• ❤️ Gérer votre liste de souhaits\n"
            "• ⭐ Laisser des avis\n"
            "• 💡 Obtenir des recommandations\n\n"
            "Demandez-moi n'importe quoi ! / Ask me anything!"
        ),

        "help": (
            "📋 Voici tout ce que je peux faire / Here's everything I can do:\n\n"
            "🔍 Rechercher : 'Montrer les peintures' / 'Show me paintings'\n"
            "🎨 Par type : 'Sculptures' / 'Digital art' / 'Photographies'\n"
            "🏷️ Par catégorie : 'Art abstrait' / 'Paysages' / 'Portraits'\n"
            "👨‍🎨 Par artiste : 'Art de [nom]' / 'Art by [name]'\n"
            "💰 Par prix : 'Art pas cher' / 'Art under 500'\n"
            "🆕 Nouveautés : 'Quoi de neuf ?' / 'New arrivals'\n"
            "🔨 Enchères : 'Voir les enchères' / 'How to bid'\n"
            "📦 Commandes : 'Mes commandes' / 'My orders'\n"
            "❤️ Favoris : 'Ma liste de souhaits' / 'My wishlist'\n"
            "🛒 Panier : 'Comment acheter' / 'How to checkout'\n"
            "⭐ Avis : 'Comment noter' / 'How to review'\n"
            "💳 Paiement : 'Comment payer' / 'Payment methods'\n"
            "📦 Livraison : 'Info livraison' / 'Shipping info'\n"
            "💡 Recommandations : 'Conseille moi' / 'Suggest something'"
        ),

        "search_products": "__DYNAMIC__",
        "search_paintings": "__DYNAMIC__",
        "search_sculptures": "__DYNAMIC__",
        "search_digital": "__DYNAMIC__",
        "search_photography": "__DYNAMIC__",
        "search_drawings": "__DYNAMIC__",
        "search_by_price_cheap": "__DYNAMIC__",
        "search_by_price_expensive": "__DYNAMIC__",
        "search_by_artist": "__DYNAMIC__",
        "search_by_category": "__DYNAMIC__",
        "search_newest": "__DYNAMIC__",
        "view_auctions": "__DYNAMIC__",
        "order_history": "__DYNAMIC__",
        "recommend": "__DYNAMIC__",
        "wishlist_view": "__DYNAMIC__",
        "cart_view": "__DYNAMIC__",
        "track_order": "__DYNAMIC__",
        "compare_prices": "__DYNAMIC__",
        "budget_recommend": "__DYNAMIC__",

        "bidding_help": (
            "🔨 Comment enchérir / How to Bid:\n\n"
            "1️⃣ Trouvez un produit avec le tag 'auction' dans le catalogue\n"
            "    Find a product tagged 'auction' in the catalogue\n\n"
            "2️⃣ Cliquez sur '⚡ Enchérir' sur la carte du produit\n"
            "    Click '⚡ Enchérir' on the product card\n\n"
            "3️⃣ Consultez l'historique des enchères et le prix actuel\n"
            "    Check the bid history and current price\n\n"
            "4️⃣ Entrez un montant supérieur à l'enchère actuelle\n"
            "    Enter an amount higher than the current bid\n\n"
            "5️⃣ Cliquez 'Placer l'enchère' pour confirmer\n"
            "    Click 'Place bid' to confirm\n\n"
            "💡 Conseils / Tips:\n"
            "• Le prix de réserve doit être atteint / Reserve price must be met\n"
            "• Vous pouvez enchérir plusieurs fois / You can bid multiple times\n"
            "• Le plus haut enchérisseur gagne ! / Highest bidder wins!"
        ),

        "cart_help": (
            "🛒 Guide Panier & Achat / Cart & Purchase Guide:\n\n"
            "1️⃣ Parcourez le catalogue et trouvez une oeuvre\n"
            "    Browse the catalogue and find artwork you love\n\n"
            "2️⃣ Cliquez '🛒 Acheter' pour les produits à prix fixe\n"
            "    Click '🛒 Acheter' for fixed-price products\n\n"
            "3️⃣ Confirmez votre nom et cliquez 'Confirmer l'achat'\n"
            "    Confirm your name and click 'Confirm purchase'\n\n"
            "4️⃣ Vous serez redirigé vers Stripe pour le paiement sécurisé\n"
            "    You'll be redirected to Stripe for secure payment\n\n"
            "5️⃣ Une fois payé, la commande est enregistrée automatiquement\n"
            "    Once paid, the order is recorded automatically\n\n"
            "💡 Les enchères se font via le bouton '⚡ Enchérir'\n"
            "    Auctions use the '⚡ Enchérir' button instead"
        ),

        "wishlist_help": (
            "❤️ Liste de Souhaits / Wishlist:\n\n"
            "Pour sauvegarder une oeuvre / To save artwork:\n"
            "• Cliquez le bouton '♥ Souhait' sur la carte du produit\n"
            "  Click the '♥ Souhait' button on the product card\n\n"
            "Pour voir vos favoris / To view your favorites:\n"
            "• Allez dans 'LISTE DE SOUHAITS' dans le menu à gauche\n"
            "  Go to 'LISTE DE SOUHAITS' in the left sidebar\n\n"
            "⚠️ Les produits sauvegardés peuvent être vendus,\n"
            "   n'attendez pas trop longtemps !\n"
            "   Saved items may sell, don't wait too long!"
        ),

        "about_marketplace": (
            "🏛️ À Propos du Marketplace / About the Marketplace:\n\n"
            "Notre marketplace est une plateforme d'art en ligne où les artistes\n"
            "vendent leurs créations directement aux collectionneurs.\n\n"
            "Our marketplace is an online art platform where artists\n"
            "sell their creations directly to collectors.\n\n"
            "✨ Fonctionnalités / Features:\n"
            "• Oeuvres originales d'artistes talentueux\n"
            "• Achats à prix fixe et enchères en direct\n"
            "• Description d'image par IA\n"
            "• Paiement sécurisé via Stripe\n"
            "• Liste de souhaits et suivi de commandes\n"
            "• Système d'avis et de notation"
        ),

        "thanks": (
            "De rien, {user_name} ! 🌟 N'hésitez pas si vous avez d'autres questions.\n"
            "You're welcome! Feel free to ask anything else."
        ),

        "goodbye": (
            "Au revoir, {user_name} ! 👋 À bientôt sur le marketplace !\n"
            "Goodbye! See you soon on the marketplace! ✨"
        ),

        "compliment": (
            "Merci beaucoup ! 🌟 Je suis là pour vous aider.\n"
            "Thank you so much! I'm here to help. What else can I do for you?"
        ),

        "negative": (
            "Désolé pour la confusion ! 😔 Pouvez-vous reformuler ?\n"
            "Sorry for the confusion! Could you rephrase?\n\n"
            "Vous pouvez demander / You can ask:\n"
            "• Rechercher des produits / Search products\n"
            "• Voir les enchères / View auctions\n"
            "• Consulter vos commandes / Check orders\n"
            "• Obtenir de l'aide / Get help"
        ),

        "product_detail": (
            "📋 Détails Produit / Product Details:\n\n"
            "Pour voir les détails d'un produit, regardez sa carte dans le catalogue.\n"
            "To see product details, look at its card in the catalogue.\n\n"
            "Chaque carte affiche / Each card shows:\n"
            "• 📸 Image de l'oeuvre / Artwork image\n"
            "• 🏷️ Nom et artiste / Name and artist\n"
            "• 💰 Prix / Price\n"
            "• 📝 Description\n"
            "• 🎨 Type (Peinture, Sculpture, etc.)\n"
            "• 🔨 Type de vente (Fixe ou Enchère)"
        ),

        "review_guide": (
            "⭐ Comment laisser un avis / How to Leave a Review:\n\n"
            "1️⃣ Trouvez le produit dans le catalogue\n"
            "    Find the product in the catalogue\n\n"
            "2️⃣ Cliquez le bouton '★ Avis' sur la carte\n"
            "    Click the '★ Avis' button on the card\n\n"
            "3️⃣ Choisissez une note de 1 à 5 étoiles\n"
            "    Choose a rating from 1 to 5 stars\n\n"
            "4️⃣ Ajoutez un commentaire (optionnel)\n"
            "    Add a comment (optional)\n\n"
            "5️⃣ Cliquez 'Envoyer l'avis'\n"
            "    Click 'Submit review'\n\n"
            "💡 Vos avis aident les artistes et les autres acheteurs !\n"
            "   Your reviews help artists and other buyers!"
        ),

        "shipping_info": (
            "📦 Livraison / Shipping Info:\n\n"
            "Notre marketplace propose différents types d'art :\n\n"
            "• Art Numérique : Téléchargement après achat\n"
            "  Digital Art: Download available after purchase\n\n"
            "• Art Physique : L'artiste vous contactera pour la livraison\n"
            "  Physical Art: The artist will contact you for delivery\n\n"
            "• Enchères gagnées : L'artiste est notifié automatiquement\n"
            "  Auction wins: The artist is notified automatically\n\n"
            "💡 Consultez les détails de votre commande pour plus d'infos.\n"
            "   Check your order details for more information."
        ),

        "payment_help": (
            "💳 Paiement / Payment Info:\n\n"
            "Nous utilisons Stripe pour des paiements 100% sécurisés.\n"
            "We use Stripe for 100% secure payments.\n\n"
            "🔒 Comment ça marche / How it works:\n"
            "1️⃣ Cliquez 'Acheter' sur un produit\n"
            "2️⃣ Confirmez l'achat dans la fenêtre\n"
            "3️⃣ Stripe s'ouvre dans votre navigateur\n"
            "4️⃣ Entrez vos informations de carte\n"
            "5️⃣ Le paiement est traité en toute sécurité\n\n"
            "💡 Cartes acceptées / Accepted: Visa, Mastercard, etc.\n"
            "⚠️ Les enchères n'utilisent pas Stripe directement.\n"
            "   Auctions don't use Stripe directly."
        ),

        "account_help": (
            "👤 Compte / Account Info:\n\n"
            "🔑 Connexion / Login:\n"
            "• Entrez votre nom et sélectionnez votre rôle (Client)\n"
            "  Enter your name and select your role (Client)\n\n"
            "👤 Votre session / Your session:\n"
            "• Votre nom apparaît en bas du menu à gauche\n"
            "  Your name appears at the bottom of the left sidebar\n\n"
            "🚪 Déconnexion / Logout:\n"
            "• Cliquez 'Déconnexion' en bas du menu\n"
            "  Click 'Déconnexion' at the bottom of the sidebar"
        ),
    }
}

# ============================================================
# PURE PYTHON TF-IDF ENGINE
# ============================================================

def tokenize(text):
    return re.findall(r'[a-zàâäéèêëïîôùûüÿçœæ]+', text.lower())

def compute_tf(tokens):
    tf = Counter(tokens)
    total = len(tokens) if tokens else 1
    return {word: count / total for word, count in tf.items()}

def compute_idf(documents):
    n_docs = len(documents)
    df = defaultdict(int)
    for doc in documents:
        seen = set(tokenize(doc))
        for word in seen:
            df[word] += 1
    idf = {}
    for word, count in df.items():
        idf[word] = math.log((n_docs + 1) / (count + 1)) + 1
    return idf

def compute_tfidf_vector(text, idf):
    tokens = tokenize(text)
    tf = compute_tf(tokens)
    vector = {}
    for word in set(tokens):
        if word in idf:
            vector[word] = tf.get(word, 0) * idf[word]
    return vector

def cosine_similarity(vec1, vec2):
    common = set(vec1.keys()) & set(vec2.keys())
    if not common:
        return 0.0
    dot = sum(vec1[k] * vec2[k] for k in common)
    norm1 = math.sqrt(sum(v * v for v in vec1.values()))
    norm2 = math.sqrt(sum(v * v for v in vec2.values()))
    if norm1 == 0 or norm2 == 0:
        return 0.0
    return dot / (norm1 * norm2)

def train_model(context):
    samples = []
    labels = []
    for intent, phrases in TRAINING_DATA.get(context, {}).items():
        for phrase in phrases:
            samples.append(phrase)
            labels.append(intent)

    idf = compute_idf(samples)

    vectors = []
    for sample in samples:
        vec = compute_tfidf_vector(sample, idf)
        vectors.append(vec)

    intent_vectors = defaultdict(lambda: defaultdict(float))
    intent_counts = Counter(labels)

    for vec, label in zip(vectors, labels):
        for word, value in vec.items():
            intent_vectors[label][word] += value

    for intent in intent_vectors:
        count = intent_counts[intent]
        for word in intent_vectors[intent]:
            intent_vectors[intent][word] /= count

    # Evaluate accuracy
    correct = 0
    for sample, true_label in zip(samples, labels):
        vec = compute_tfidf_vector(sample, idf)
        best_intent = None
        best_score = -1
        for intent, centroid in intent_vectors.items():
            score = cosine_similarity(vec, centroid)
            if score > best_score:
                best_score = score
                best_intent = intent
        if best_intent == true_label:
            correct += 1

    accuracy = correct / len(samples) * 100 if samples else 0

    model_data = {
        'idf': dict(idf),
        'centroids': {intent: dict(vec) for intent, vec in intent_vectors.items()},
        'intents': list(set(labels)),
        'n_samples': len(samples),
        'n_intents': len(set(labels)),
        'accuracy': round(accuracy, 2)
    }
    return model_data


def main():
    models_dir = os.path.join(os.path.dirname(__file__), 'models')
    os.makedirs(models_dir, exist_ok=True)

    results = {}

    for context in ['client']:
        print(f"Training {context} model...")
        model_data = train_model(context)

        model_path = os.path.join(models_dir, f'{context}_model.json')
        with open(model_path, 'w', encoding='utf-8') as f:
            json.dump(model_data, f, ensure_ascii=False)

        results[context] = {
            'intents': model_data['n_intents'],
            'training_samples': model_data['n_samples'],
            'accuracy': model_data['accuracy'],
            'model_path': model_path
        }
        print(f"  > {model_data['n_intents']} intents, {model_data['n_samples']} samples, accuracy: {model_data['accuracy']}%")

    # Save responses
    responses_path = os.path.join(models_dir, 'responses.json')
    with open(responses_path, 'w', encoding='utf-8') as f:
        json.dump(RESPONSES, f, ensure_ascii=False, indent=2)

    # Save training data
    training_path = os.path.join(models_dir, 'training_data.json')
    with open(training_path, 'w', encoding='utf-8') as f:
        json.dump(TRAINING_DATA, f, ensure_ascii=False, indent=2)

    print("\n[OK] Training complete!")
    print(json.dumps({"success": True, "results": results}, indent=2))


if __name__ == '__main__':
    main()
