import argparse
import time
import requests
from threading import Thread, Lock

from fastapi import FastAPI, HTTPException, status, Request
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
import socket
import uvicorn
from fastapi import FastAPI
from typing import List
from dto.resPeerDto import ResPeerDto
from dto.resChunkDto import ResChunkDto
from dto.resFileDto import ResFileDto
from dto.reqFileDto import ReqFileDto
from dto.reqPeerDto import ReqPeerDto

from server_utils import *

import logging

import sys

logging.basicConfig(
    format="%(levelname)s: %(message)s",
    level=logging.DEBUG,  
    stream=sys.stdout, 
    force=True  
)


DB_PATH = "./database/p2p_database.db"
TIME_OUT = 3
HEART_BEAT_INTERVAL = 60

peers_lock = Lock()  # global lock for active_peers
app = FastAPI()       # FastAPI app

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def parse_args():
    parser = argparse.ArgumentParser(
        prog='server',
        description='p2p server'
    )
    parser = argparse.ArgumentParser(description="Run FastAPI server")
    parser.add_argument("hostname", help="Hostname of the server")
    parser.add_argument("port", type=int, help="Port where server listens")
    parser.add_argument("-d", "--debug", action="store_true", help="Enable debugging output")
    return parser.parse_args()

def run_heartbeat_loop(active_peers):
    """
    Infinite loop to call heart_check() every HEART_BEAT_INTERVAL seconds.
    """
    while True:
        heart_check(active_peers)
        time.sleep(HEART_BEAT_INTERVAL)

def heart_check(active_peers):
    """
    Sends a TCP ping request to each peer in active_peers.
    """
    logging.info("HeartCheck: Checking all peers...")

    peers_copy = list(active_peers) 
    logging.info(f"Peers: {peers_copy}")

    for peer in peers_copy[:]:  # Iterate over copy
        ip, port = peer  # Unpack tuple (peer_ip, peer_port)
        
        try:
            with socket.create_connection((ip, port), timeout=TIME_OUT) as s:
                s.sendall(b"ping      \n") # 10 bytes Message Protocol
                response = s.recv(1024).decode().strip()  

                if response == "pong":
                    logging.info(f"Peer {ip}:{port} is alive.")
                else:
                    logging.warning(f"Peer {ip}:{port} sent unexpected response '{response}'. Removing.")
                    peers_copy.remove(peer)
        except (socket.timeout, ConnectionRefusedError, OSError) as e:
            logging.warning(f"Peer {ip}:{port} not responding ({e}). Removing.")
            peers_copy.remove(peer)

    # Update the original set with a lock
    with peers_lock:
        active_peers.clear()
        active_peers.update(peers_copy)

    return active_peers

def add_active_peer(active_peers, peer_ip, peer_port):
    """
    Adds a new peer to active_peers, protected by peers_lock.
    """
    new_peer = (peer_ip, peer_port) 
    with peers_lock:
        if new_peer not in active_peers:
            active_peers.add(new_peer)
            logging.info(f"Added peer: {peer_ip}:{peer_port}")
            return True
        return False
    
def remove_active_peer(active_peers, peer_ip, peer_port):
    """
    Removes a peer from active_peers, protected by peers_lock.
    """
    old_peer = (peer_ip, peer_port)  
    with peers_lock:
        if old_peer in active_peers:
            active_peers.remove(old_peer)
            logging.info(f"Removed peer: {peer_ip}:{peer_port}")
            return True
        return False

def server_init():
    """
    1) Create tables
    2) Load existing peers from DB
    3) Return a set of active peers
    """
    create_tables(DB_PATH)

    # load peers from DB, then convert to dict format
    loaded_peers = get_all_peers(DB_PATH)  # returns list of (ip, port)
    active_peers = heart_check(set(loaded_peers))  # remove dead peers

    logging.info("Server initialized")
    return active_peers

# -------------------------------------------------------------------------
# FASTAPI ROUTES
# -------------------------------------------------------------------------


@app.post("/peers/add")
async def api_add_peer(peer : ReqPeerDto):
    """
    Add a new peer to the active_peers set.
    """ 
    if not hasattr(app.state, "active_peers"):
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Server error: active peers not initialized"
        )

    ip = peer.peer_ip
    port = peer.peer_port

    if add_active_peer(app.state.active_peers, ip, port):
        return {"status": "success", "message": f"Peer {ip}:{port} added."}
    
    return JSONResponse(
        status_code=status.HTTP_409_CONFLICT,
        content={"status": "error", "message": f"Peer {ip}:{port} already exists."}
    )

@app.post("/peers/remove")
async def api_remove_peer(peer : ReqPeerDto):
    """
    Remove a peer from active_peers.
    """

    if not hasattr(app.state, "active_peers"):
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Server error: active peers not initialized"
        )

    ip = peer.peer_ip
    port = peer.peer_port

    if remove_active_peer(app.state.active_peers, ip, port):
        return {"status": "success", "message": f"Peer {ip}:{port} removed."}
    
    return JSONResponse(
        status_code=status.HTTP_404_NOT_FOUND, 
        content={"status": "error", "message": f"Peer {ip}:{port} not found."}
    )


@app.get("/peers", response_model=List[ResPeerDto])
async def api_get_peers():
    try:
        if not hasattr(app.state, "active_peers"):
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Server error: active peers not initialized"
            )

        with peers_lock:
            # Convert set of tuples to list of ResPeerDto
            peer_list = [ResPeerDto(peer_ip=peer[0], peer_port=peer[1]) for peer in app.state.active_peers]

        return JSONResponse(
            status_code=status.HTTP_200_OK,
            content=[peer.model_dump() for peer in peer_list]
        )

    except Exception as e:
        print(f"Unexpected error in api_get_peers: {e}")
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={"error": "An unexpected server error occurred"}
        )

@app.get("/files")  
async def api_get_all_files():
    """
    Retrieve all files asynchronously from the database and return as JSON.
    """

    try:
        files = await get_all_files(DB_PATH) 
        return files
    except Exception as e:
        print(f"Error retrieving file list: {e}")
        raise HTTPException(status_code=500, detail=str(e))
    

@app.post("/files/upload_file")
async def api_upload_file(file_info: ReqFileDto):
    """
    Asynchronously adds a new file to the 'files' table.
    - If file_chunks is provided, it stores chunk data and peer locations.
    - If file_chunks is missing, only the file entry is created.
    """


    try:
        # Insert file metadata into the database
        file_id = await insert_file(
            DB_PATH,
            file_info.file_name,
            file_info.file_hash,
            file_info.file_size,
            file_info.file_enc
        )

        # If no file_chunks provided, return early
        if not file_info.file_chunks:
            return JSONResponse(
                status_code=200,
                content={
                    "file_id": file_id,
                    "file_name": file_info.file_name,
                    "file_hash": file_info.file_hash,
                    "message": "File uploaded without chunks"
                }
            )

        # Process file chunks
        total_number_of_chunks = 0
        chunks = []
        peers = {}

        for chunk in file_info.file_chunks:
            if chunk.chunk_index >= total_number_of_chunks:
                total_number_of_chunks = chunk.chunk_index + 1

            chunks.append({
                "file_id": file_id,
                "chunk_index": chunk.chunk_index,
                "chunk_hash": chunk.chunk_hash,
                "chunk_size": chunk.chunk_size
            })

            if chunk.chunk_index not in peers:
                peers[chunk.chunk_index] = []

            # {{chunk_index_1: [peer1, peer2, peer3]}, {chunk_index_2: [peer1, peer2]}}
            if chunk.chunk_peers:
                for peer in chunk.chunk_peers:
                    peers[chunk.chunk_index].append({
                        "peer_ip": peer.peer_ip,
                        "peer_port": peer.peer_port
                    })

        # Track uploaded chunks
        chunk_upload_status = [False] * total_number_of_chunks

        # Insert chunks and peers
        for chunk in chunks:
            if not chunk_upload_status[chunk["chunk_index"]]:
                chunk_id = await insert_chunk(
                    DB_PATH,
                    chunk["file_id"],
                    chunk["chunk_index"],
                    chunk["chunk_hash"],
                    chunk["chunk_size"]
                )

                for peer in peers.get(chunk["chunk_index"], []):
                    await insert_peer(DB_PATH, chunk_id, peer["peer_ip"], peer["peer_port"])

                chunk_upload_status[chunk["chunk_index"]] = True

        return JSONResponse(
            status_code=200,
            content={
                "file_id": file_id,
                "file_name": file_info.file_name,
                "file_hash": file_info.file_hash,
                "message": "File and chunks uploaded successfully"
            }
        )

    except Exception as e:
        logging.error(f"Error while uploading file: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to upload file: '{file_info.file_name}'"
        )

# @app.post("/files/upload_chunks")
# async def api_upload_file(request: ReqFileDto):
#     return None


@app.get("/files/download_file/{file_id}", response_model=ResFileDto)
async def api_download_file(file_id: int) -> ResFileDto:
    """
    Retrieves file details along with its chunks and active peers.
    """

    try:
        file_data = await get_file_by_id(DB_PATH, file_id)
        if not file_data:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"File with ID {file_id} not found"
            )

        # Get chunks
        chunk_list = await get_chunks_by_file(DB_PATH, file_data["file_id"])

        # Get active peers
        if not hasattr(app.state, "active_peers"):
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Server error: active peers not initialized"
            )

        active_peers = app.state.active_peers
        # active_peers = set([
        #     ('192.168.1.11', 5002), 
        #     ('192.168.1.133', 5001), 
        #     ('192.168.1.1333', 5001), 
        #     ('192.168.1.10', 5001), 
        #     ('192.168.1.1332352353', 5001)
        # ])

        # Fetch peer details for each chunk
        file_chunks = []
        for chunk in chunk_list:
            chunk_peers = await get_peers_by_chunk(DB_PATH, chunk["chunk_id"], active_peers)
            file_chunks.append(ResChunkDto(
                chunk_id=chunk["chunk_id"],
                chunk_index=chunk["chunk_index"],
                chunk_hash=chunk["chunk_hash"],
                chunk_size=chunk["chunk_size"],
                chunk_peers=chunk_peers
            ))

        return ResFileDto(
            file_id=file_data["file_id"],
            file_name=file_data["file_name"],
            file_hash=file_data["file_hash"],
            file_size=file_data["file_size"],
            file_enc=file_data["file_enc"],
            file_chunks=file_chunks
        )

    except HTTPException as http_err:
        raise http_err

    except Exception as e:
        logging.info(f"Unexpected error while retrieving file data: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Unexpected server error while retrieving file data"
        )

# -------------------------------------------------------------------------
# MAIN / ENTRY POINT
# -------------------------------------------------------------------------

def main():
    args = parse_args()
    logging.info.enabled = args.debug

    # Initialize server and get the active_peers set
    active_peers = server_init()

    # Attach active_peers to the FastAPI app so routes can access it
    app.state.active_peers = active_peers

    # Start the heartbeat loop in a daemon thread
    t = Thread(target=run_heartbeat_loop, args=(active_peers,), daemon=True)
    t.start()


    uvicorn_logger = logging.getLogger("uvicorn")
    uvicorn_logger.setLevel(logging.DEBUG if args.debug else logging.INFO)
    uvicorn.run(app, host=args.hostname, port=args.port, log_level="debug" if args.debug else "info")


if __name__ == "__main__":
    main()