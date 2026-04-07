# python3 find_large_drawables.py
import os

# Pfad zum Projektordner, ggf. anpassen
project_root = os.path.abspath(".")

# Basisordner für Drawables
res_path = os.path.join(project_root, "app", "src", "main", "res")

# Maximale Dateigröße in Bytes (z.B. 64 KB)
MAX_SIZE = 64 * 1024

def find_large_drawables(path, max_size):
    large_files = []
    # Durchlaufe alle res/drawable* Ordner
    for root, dirs, files in os.walk(path):
        if os.path.basename(root).startswith("drawable"):
            for file in files:
                file_path = os.path.join(root, file)
                try:
                    size = os.path.getsize(file_path)
                    if size > max_size:
                        large_files.append((file_path, size))
                except OSError:
                    pass
    return large_files

if __name__ == "__main__":
    results = find_large_drawables(res_path, MAX_SIZE)
    if results:
        print(f"Gefundene Drawable-Dateien größer als {MAX_SIZE} Bytes:")
        for fpath, size in results:
            print(f"{fpath} - {size / 1024:.2f} KB")
    else:
        print(f"Keine Drawable-Dateien größer als {MAX_SIZE} Bytes gefunden.")
