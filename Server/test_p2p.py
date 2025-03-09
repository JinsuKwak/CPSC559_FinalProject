import requests
import time
import json

# FastAPI server details
BASE_URL = "http://localhost:33333"

# Sample file data
FILE_DATA = {
    "file_name": "test_video.mp4",
    "file_hash": "123456789abcdef",
    "file_size": 104857600,  # 100MB
    "file_enc": False,
    "file_chunks": [
        {
            "chunk_index": 0,
            "chunk_hash": "chunk_hash_0",
            "chunk_size": 10485760,  # 10MB
            "chunk_peers": [
                {"peer_ip": "192.168.1.10", "peer_port": 5001},
                {"peer_ip": "192.168.1.133", "peer_port": 5001},
                {"peer_ip": "192.168.1.1333", "peer_port": 5001},
                {"peer_ip": "192.168.1.1333", "peer_port": 5001},
                {"peer_ip": "192.168.1.1332352353", "peer_port": 5001}            ]
        },
        {
            "chunk_index": 5,
            "chunk_hash": "chunk_hash_1",
            "chunk_size": 10485760,
            "chunk_peers": [
                {"peer_ip": "192.168.1.11", "peer_port": 5002}
            ]
        },
                {
            "chunk_index": 9,
            "chunk_hash": "chunk_hash_1",
            "chunk_size": 10485760,
            "chunk_peers": [
                {"peer_ip": "192.168.1.11", "peer_port": 5002}
            ]
        },
        {
            "chunk_index": 8,
            "chunk_hash": "chunk_hash_1",
            "chunk_size": 10485760,
            "chunk_peers": [
            ]
        },
        
    ]
}

# FILE_DATA = {
#     "file_name": "test_video.mp4",
#     "file_hash": "123456789abcdef",
#     "file_size": 104857600,  # 100MB
#     "file_enc": False,
#     "file_chunks": [

#                 {
#             "chunk_index": 9,
#             "chunk_hash": "chunk_hash_1",
#             "chunk_size": 10485760,
#             "chunk_peers": [
#                 {"peer_ip": "192.168.1.113", "peer_port": 500332}
#             ]
#         },
#         {
#             "chunk_index": 8,
#             "chunk_hash": "chunk_hash_1",
#             "chunk_size": 10485760,
#             "chunk_peers": [
#                 {"peer_ip": "192.168.1.11", "peer_port": 5002}
#             ]
#         },
        
#     ]
# }
def test_upload():
    """
    Test uploading a file to the P2P system.
    """
    url = f"{BASE_URL}/files/upload_file"
    print(f"Uploading file to {url}...")
    
    start_time = time.time()
    response = requests.post(url, json=FILE_DATA)
    end_time = time.time()

    if response.status_code == 200:
        file_id = response.json().get("file_id")
        print(f"✅ Upload successful! File ID: {file_id} (Time: {end_time - start_time:.2f} sec)")
        return file_id
    else:
        print(f"❌ Upload failed: {response.text}")
        return None

def test_download(file_id):
    """
    Test downloading a file from the P2P system.
    """
    url = f"{BASE_URL}/files/download_file/{file_id}"
    print(f"\nDownloading file from {url}...")

    start_time = time.time()
    response = requests.get(url)
    end_time = time.time()

    if response.status_code == 200:
        file_data = response.json()
        print(f"✅ Download successful! (Time: {end_time - start_time:.2f} sec)")
        print(json.dumps(file_data, indent=4))
    else:
        print(f"❌ Download failed: {response.text}")

if __name__ == "__main__":
    file_id = test_upload()
    if file_id:
        test_download(file_id)