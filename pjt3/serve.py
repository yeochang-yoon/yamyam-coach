"""Static development server with consistent JavaScript MIME types on Windows."""
import argparse
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8033)
    args = parser.parse_args()
    SimpleHTTPRequestHandler.extensions_map.update({".js": "text/javascript", ".css": "text/css", ".json": "application/json"})
    handler = partial(SimpleHTTPRequestHandler, directory=str(Path(__file__).parent / "web"))
    print(f"Yamyam PJT3: http://127.0.0.1:{args.port}", flush=True)
    ThreadingHTTPServer(("127.0.0.1", args.port), handler).serve_forever()
