import aiosqlite
import sqlite3
from fastapi import HTTPException, status

from dto.resPeerDto import ResPeerDto

__all__ = [
    "create_tables",
    "insert_file",
    "insert_chunk",
    "insert_peer",
    "get_file_by_id",
    "get_chunks_by_file",  
    "get_peers_by_chunk",
    "get_all_peers",
    "get_all_files"
]



def create_tables(db_path="database/p2p_database.db"):
    """
    Creates the necessary P2P tables (files, chunks, peers) in the database.
    - files: stores basic file info
    - chunks: stores chunk info for each file
    - peers: stores which peer (ip:port) has which chunk
    """
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    # Create 'files' table
    cur.execute("""
    CREATE TABLE IF NOT EXISTS files (
        file_id INTEGER PRIMARY KEY AUTOINCREMENT,
        file_name TEXT NOT NULL,
        file_hash TEXT NOT NULL,
        file_size BIGINT NOT NULL,
        file_enc BOOLEAN NOT NULL
    );
    """)

    # Create 'chunks' table
    cur.execute("""
    CREATE TABLE IF NOT EXISTS chunks (
        chunk_id INTEGER PRIMARY KEY AUTOINCREMENT,
        file_id INTEGER,
        chunk_index INTEGER NOT NULL,
        chunk_hash TEXT NOT NULL,
        chunk_size INTEGER NOT NULL,
        FOREIGN KEY(file_id) REFERENCES files(file_id)
    );
    """)

    # Create 'peers' table
    cur.execute("""
    CREATE TABLE IF NOT EXISTS peers (
        peer_id INTEGER PRIMARY KEY AUTOINCREMENT,
        chunk_id INTEGER,
        peer_ip TEXT,
        peer_port INTEGER,
        FOREIGN KEY(chunk_id) REFERENCES chunks(chunk_id)
    );
    """)

    conn.commit()
    conn.close()
    print("Database tables (files, chunks, peers) have been created:", db_path)


async def insert_file(db_path, file_name, file_hash, file_size, file_enc):
    """
    Asynchronously inserts a new file record into the 'files' table.
    Ensures that (file_name, file_hash, file_size, file_enc) is unique before inserting.
    Returns the file_id if successful, or the existing file_id if already present.
    """
    file_id = None
    try:
        async with aiosqlite.connect(db_path) as conn:
            # Check if the file already exists
            cur = await conn.execute("""
                SELECT file_id FROM files 
                WHERE file_name = ? AND file_hash = ? AND file_size = ? AND file_enc = ?
            """, (file_name, file_hash, file_size, file_enc))
            row = await cur.fetchone()

            # If file already exists, return the existing file_id
            if row:
                print(f"[files] Skipped (already exists): {file_name} (hash={file_hash}), file_id={row[0]}")
                return row[0]  # Return existing file_id

            # If no duplicate found, insert into DB
            cur = await conn.execute("""
                INSERT INTO files (file_name, file_hash, file_size, file_enc)
                VALUES (?, ?, ?, ?)
            """, (file_name, file_hash, file_size, file_enc))

            await conn.commit()
            file_id = cur.lastrowid  # Get the last inserted file_id

            print(f"[files] Inserted: {file_name} (hash={file_hash}), file_id={file_id}")

    except Exception as e:
        print(f"Error inserting file: {e}")
        raise HTTPException(
            status_code=500,
            detail="Database error while inserting file"
        )

    return file_id


async def insert_chunk(db_path, file_id, chunk_index, chunk_hash, chunk_size):
    """
    Asynchronously inserts a new chunk record into the 'chunks' table.
    Ensures that (file_id, chunk_index, chunk_hash) is unique before inserting.
    Returns the chunk_id if successful, or the existing chunk_id if already present.
    """
    chunk_id = None
    try:
        async with aiosqlite.connect(db_path) as conn:
            # Check if the chunk already exists
            cur = await conn.execute("""
                SELECT chunk_id FROM chunks 
                WHERE file_id = ? AND chunk_index = ? AND chunk_hash = ?
            """, (file_id, chunk_index, chunk_hash))
            row = await cur.fetchone()

            # If chunk already exists, return the existing chunk_id
            if row:
                print(f"[chunks] Skipped (already exists): chunk_index={chunk_index}, chunk_hash={chunk_hash}, chunk_id={row[0]}")
                return row[0]  # Return existing chunk_id

            # If no duplicate found, insert into DB
            cur = await conn.execute("""
                INSERT INTO chunks (file_id, chunk_index, chunk_hash, chunk_size)
                VALUES (?, ?, ?, ?)
            """, (file_id, chunk_index, chunk_hash, chunk_size))

            await conn.commit()
            chunk_id = cur.lastrowid  # Get the last inserted chunk_id

            print(f"[chunks] Inserted: chunk_index={chunk_index}, chunk_hash={chunk_hash}, chunk_id={chunk_id}")

    except Exception as e:
        print(f"Error inserting chunk: {e}")
        raise HTTPException(
            status_code=500,
            detail="Database error while inserting chunk"
        )

    return chunk_id

async def insert_peer(db_path, chunk_id, peer_ip, peer_port):
    """
    Asynchronously inserts a peer record that holds a specific chunk.
    Ensures that (chunk_id, peer_ip, peer_port) is unique before inserting.
    """
    try:
        async with aiosqlite.connect(db_path) as conn:
            cur = await conn.execute("""
                SELECT COUNT(*) FROM peers 
                WHERE chunk_id = ? AND peer_ip = ? AND peer_port = ?
            """, (chunk_id, peer_ip, peer_port))
            count = await cur.fetchone()
            
            if count and count[0] > 0:
                print(f"[peers] Skipped (already exists): chunk_id={chunk_id}, peer={peer_ip}:{peer_port}")
                return  # Exit without inserting

            # If no duplicate found, insert into DB
            await conn.execute("""
                INSERT INTO peers (chunk_id, peer_ip, peer_port)
                VALUES (?, ?, ?)
            """, (chunk_id, peer_ip, peer_port))

            await conn.commit()
            print(f"[peers] Inserted: chunk_id={chunk_id}, peer={peer_ip}:{peer_port}")

    except Exception as e:
        print(f"Error inserting peer: {e}")
        raise


async def get_file_by_id(db_path: str, file_id: int):
    """
    Retrieves a file from the 'files' table by its file_id.
    Returns None if the file is not found.
    """
    try:
        async with aiosqlite.connect(db_path) as conn:
            cur = await conn.execute("""
                SELECT file_id, file_name, file_hash, file_size, file_enc 
                FROM files WHERE file_id = ?
            """, (file_id,))
            row = await cur.fetchone()

        if row:
            return {
                "file_id": row[0],  
                "file_name": row[1],
                "file_hash": row[2],
                "file_size": row[3],
                "file_enc": bool(row[4])
            }
        return None

    except Exception as e:
        print(f"Database error while retrieving file: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Database error while retrieving file"
        )


async def get_chunks_by_file(db_path: str, file_id: int):
    """
    Retrieves all chunks associated with a given file_id.
    Returns a list of dictionaries with chunk details.
    - Includes `chunk_id` for internal use but does NOT return it in the response.
    """
    try:
        async with aiosqlite.connect(db_path) as conn:
            cur = await conn.execute("""
                SELECT chunk_id, chunk_index, chunk_hash, chunk_size
                FROM chunks 
                WHERE file_id = ?
                ORDER BY chunk_index ASC
            """, (file_id,))
            rows = await cur.fetchall()

        if not rows:
            raise HTTPException(
                status_code=404,
                detail=f"No chunks found for file ID {file_id}"
            )

        return [
            {
                "chunk_id": row[0],  # Internal use only, NOT returned in response
                "chunk_index": row[1],
                "chunk_hash": row[2],
                "chunk_size": row[3]
            }
            for row in rows
        ]

    except Exception as e:
        print(f"Database error while retrieving chunks: {e}")
        raise HTTPException(
            status_code=500,
            detail="Database error while retrieving chunks"
        )


async def get_peers_by_chunk(db_path: str, chunk_id: int, active_peers: set):
    """
    Retrieves the list of active peers that have a given chunk.
    - Uses `chunk_id` for efficient lookup.
    - Only peers in `active_peers` are included.
    """
    try:
        async with aiosqlite.connect(db_path) as conn:
            cur = await conn.execute("""
                SELECT peer_ip, peer_port FROM peers 
                WHERE chunk_id = ?
            """, (chunk_id,))
            rows = await cur.fetchall()

        if not rows:
            return []  # No peers found, return empty list

        # Filter out only active peers
        return [
            ResPeerDto(peer_ip=row[0], peer_port=row[1])
            for row in rows if (row[0], row[1]) in active_peers
        ]

    except Exception as e:
        print(f"Database error while retrieving peers: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Database error while retrieving peers"
        )


def get_all_peers(db_path):
    """Returns a unique list of all peers found in the 'peers' table as a list of tuples (peer_ip, peer_port)."""
    conn = None
    try:
        conn = sqlite3.connect(db_path)
        cur = conn.cursor()

        sql = "SELECT DISTINCT peer_ip, peer_port FROM peers"
        cur.execute(sql)
        rows = cur.fetchall()

        return [(row[0], row[1]) for row in rows] 

    except Exception as e:
        print(f"Error retrieving peers: {e}")
        raise
    finally:
        if conn:
            conn.close()

async def get_all_files(db_path):
    """
    Asynchronously retrieves all files from the 'files' table.
    Returns a list of dictionaries containing 'file_id', 'file_name', 'file_hash', file_size and file_enc.
    """
    try:
        async with aiosqlite.connect(db_path) as conn:
            cur = await conn.execute("SELECT * FROM files")
            rows = await cur.fetchall()

        return [
            {"file_id": row[0], "file_name": row[1], "file_hash": row[2], "file_size": row[3], "file_enc": bool(row[4])}
            for row in rows
        ]

    except Exception as e:
        print(f"Error retrieving files: {e}")
        raise HTTPException(
            status_code=500,
            detail="Database error while retrieving files"
        ) 

if __name__ == "__main__":
    # Example usage
    create_tables()

    # Insert a file
    insert_file(
        db_path="database/p2p_database.db",
        file_name="test.mp4",
        file_hash="file_hash_123",
        file_size=500000,
        file_enc=True
    )

    # Insert a chunk
    new_chunk_id = insert_chunk(
        db_path="database/p2p_database.db",
        file_hash="file_hash_123",
        chunk_index=0,
        chunk_hash="chunk_hash_000",
        chunk_size=100000
    )

    # Insert a peer for that chunk
    insert_peer(
        db_path="database/p2p_database.db",
        chunk_id=new_chunk_id,
        peer_ip="192.168.0.10",
        peer_port=5001
    )

    # Insert another peer
    insert_peer(
        db_path="database/p2p_database.db",
        chunk_id=new_chunk_id,
        peer_ip="192.168.0.11",
        peer_port=5002
    )

    # Retrieve all unique peers
    unique_peers = get_all_peers("database/p2p_database.db")
    print("Unique peers:", unique_peers)
