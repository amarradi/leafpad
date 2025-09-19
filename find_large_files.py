import os

def find_large_files(root_path='.', size_threshold_kb=64):
    """
    Durchsucht rekursiv root_path nach Dateien, die größer als size_threshold_kb sind.
    Gibt Pfad und Größe aus.
    """
    size_threshold_bytes = size_threshold_kb * 1024
    large_files = []

    for dirpath, dirnames, filenames in os.walk(root_path):
        for filename in filenames:
            filepath = os.path.join(dirpath, filename)
            try:
                filesize = os.path.getsize(filepath)
                if filesize > size_threshold_bytes:
                    large_files.append((filepath, filesize))
            except OSError as e:
                print(f"Fehler beim Zugriff auf {filepath}: {e}")

    # Sortiere nach Dateigröße absteigend
    large_files.sort(key=lambda x: x[1], reverse=True)

    # Ausgabe
    print(f"Dateien größer als {size_threshold_kb} KB in '{root_path}':\n")
    for filepath, filesize in large_files:
        print(f"{filesize/1024:.1f} KB  -  {filepath}")

if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser(description="Finde große Dateien im Projekt")
    parser.add_argument('path', nargs='?', default='.', help='Startverzeichnis (Standard: aktuelles Verzeichnis)')
    parser.add_argument('--minsize', type=int, default=64, help='Mindestgröße in KB (Standard: 64)')

    args = parser.parse_args()

    find_large_files(root_path=args.path, size_threshold_kb=args.minsize)
