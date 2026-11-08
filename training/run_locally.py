#!/usr/bin/env python3
"""Run Auris_train_model.ipynb on an ordinary computer instead of Google Colab.

Only the Colab-specific lines change: the Google Drive mount, the /content paths and
the browser download at the end. Every step that organises, splits, trains, tests and
exports the model runs exactly as written in the notebook. If a line this script
expects to replace is missing, it stops rather than guessing.

    python run_locally.py NOTEBOOK OUT_DIR WORK_DIR RAW_DIR [RAW_DIR ...]

NOTEBOOK  the training notebook (Auris_train_model.ipynb)
OUT_DIR   where auris.onnx, labels.txt, model_info.json and the test report are written
WORK_DIR  scratch space for the organised images and temporary files
RAW_DIR   one or more folders of labelled images, as described in the notebook
"""
import json
import os
import sys

import matplotlib

matplotlib.use("Agg")  # no screen: figures are saved to files only


def main():
    if len(sys.argv) < 5:
        sys.exit(__doc__)
    nb_path, out_dir, work_dir, *raw_dirs = sys.argv[1:]
    out_dir, work_dir = os.path.abspath(out_dir), os.path.abspath(work_dir)
    raw_dirs = [os.path.abspath(d) for d in raw_dirs]
    os.makedirs(work_dir, exist_ok=True)

    with open(nb_path, encoding="utf-8") as f:
        cells = [c for c in json.load(f)["cells"] if c["cell_type"] == "code"]

    # (code cell number, text in the notebook, replacement)
    patches = [
        (0, "!pip -q install onnx onnxscript onnxruntime", "pass  # packages installed beforehand"),
        (1, "from google.colab import drive\n", ""),
        (1, "drive.mount('/content/drive')\n", ""),
        (1, "RAW_DIRS = ['/content/drive/MyDrive/auris_raw']", f"RAW_DIRS = {raw_dirs!r}"),
        (1, "OUT_DIR = '/content/drive/MyDrive/auris_model'", f"OUT_DIR = {out_dir!r}"),
        (2, "DATA = '/content/auris_data'", f"DATA = {os.path.join(work_dir, 'auris_data')!r}"),
        (7, "tmp_path = '/content/auris_tmp.onnx'", f"tmp_path = {os.path.join(work_dir, 'auris_tmp.onnx')!r}"),
        (7, "zip_path = '/content/auris_model.zip'", f"zip_path = {os.path.join(out_dir, 'auris_model.zip')!r}"),
        (7, "from google.colab import files\nfiles.download(zip_path)", ""),
    ]

    sources = ["".join(c["source"]) if isinstance(c["source"], list) else c["source"] for c in cells]
    for i, old, new in patches:
        found = sources[i].count(old)
        if found != 1:
            sys.exit(f"Code cell {i}: expected to find {old!r} once, found it {found} times. "
                     "The notebook has changed; update the patches in this script.")
        sources[i] = sources[i].replace(old, new)
    leftover = [i for i, s in enumerate(sources) if "/content" in s or "google.colab" in s]
    if leftover:
        sys.exit(f"Colab-only code is still present in code cells {leftover}.")

    def display(obj):
        """Jupyter's display(): print a table as text."""
        print(obj.to_string() if hasattr(obj, "to_string") else obj)

    ns = {"__name__": "__main__", "display": display}
    for i, src in enumerate(sources):
        title = src.splitlines()[0] if src else ""
        print(f"\n===== code cell {i}: {title}", flush=True)
        exec(compile(src, f"<notebook code cell {i}>", "exec"), ns)


if __name__ == "__main__":
    main()
