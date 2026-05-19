import os

os.environ["NO_PROXY"] = "localhost,127.0.0.1,::1"
os.environ["no_proxy"] = "localhost,127.0.0.1,::1"

for key in ["HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY", "http_proxy", "https_proxy", "all_proxy"]:
    os.environ.pop(key, None)

from qdrant_client import QdrantClient

COLLECTION_NAME = "products_superlinked_100k"

def main():
    client = QdrantClient(url="http://127.0.0.1:6333", timeout=300)

    info = client.get_collection(COLLECTION_NAME)

    print("collection:", COLLECTION_NAME)
    print("points_count:", info.points_count)
    print("status:", info.status)
    print("vectors_config:", info.config.params.vectors)

if __name__ == "__main__":
    main()