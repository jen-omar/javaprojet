import sys
import os
import json
from PIL import Image
from transformers import BlipProcessor, BlipForConditionalGeneration
import torch

def describe_image(image_path):
    try:
        # Load the model and processor
        # This will download about 1GB of data on first run
        model_id = "Salesforce/blip-image-captioning-large"
        processor = BlipProcessor.from_pretrained(model_id)
        model = BlipForConditionalGeneration.from_pretrained(model_id)

        # Move to GPU if available
        device = "cuda" if torch.cuda.is_available() else "cpu"
        model.to(device)

        # Open and process the image
        raw_image = Image.open(image_path).convert('RGB')

        # Unconditional image captioning
        inputs = processor(raw_image, return_tensors="pt").to(device)

        out = model.generate(**inputs, max_new_tokens=50)
        description = processor.decode(out[0], skip_special_tokens=True)

        return {
            "success": True,
            "description": description.capitalize()
        }
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({"success": False, "error": "No image path provided"}))
        sys.exit(1)

    image_path = sys.argv[1]
    if not os.path.exists(image_path):
        print(json.dumps({"success": False, "error": f"Image path does not exist: {image_path}"}))
        sys.exit(1)

    result = describe_image(image_path)
    print(json.dumps(result))
