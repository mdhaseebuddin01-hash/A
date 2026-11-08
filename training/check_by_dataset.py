"""Score the exported auris.onnx on the held-out test images with ONNX Runtime, split by source dataset."""
import json, sys, math
import numpy as np, pandas as pd, onnxruntime as ort
from PIL import Image

out_dir = sys.argv[1]
info = json.load(open(f'{out_dir}/model_info.json'))
labels = [l.strip() for l in open(f'{out_dir}/labels.txt') if l.strip()]
sess = ort.InferenceSession(f'{out_dir}/auris.onnx', providers=['CPUExecutionProvider'])
name, size = sess.get_inputs()[0].name, info['input_size']

def wilson(k, n, z=1.96):
    if n == 0: return (float('nan'),) * 2
    p = k / n; d = 1 + z*z/n; c = (p + z*z/(2*n)) / d
    h = z * math.sqrt(p*(1-p)/n + z*z/(4*n*n)) / d
    return max(0, c-h), min(1, c+h)

df = pd.read_csv(f'{out_dir}/split.csv')
df = df[df.split == 'test'].copy()
df['dataset'] = np.where(df.source.str.startswith('eardrum'), 'Turkey (eardrum)', 'Chile (Datos)')
probs = []
for p in df.path:
    im = Image.open(p).convert('RGB').resize((size, size), Image.BILINEAR)
    x = (np.asarray(im, dtype=np.float32) / 255.0).transpose(2, 0, 1)[None]
    probs.append(sess.run(None, {name: x})[0][0])
P = np.stack(probs)
df['pred'] = [labels[i] for i in P.argmax(1)]
df['conf'] = P.max(1)
df['ok'] = df.pred == df.label
df['confident'] = df.conf >= info['threshold']
print(f"ONNX Runtime {ort.__version__}; threshold {info['threshold']:.2f}; {len(df)} test images")
def line(tag, d):
    k, n = int(d.ok.sum()), len(d); lo, hi = wilson(k, n)
    c = d[d.confident]; ck, cn = int(c.ok.sum()), len(c); clo, chi = wilson(ck, cn)
    print(f"{tag:22s} all: {k:3d}/{n:<3d} = {k/max(n,1):6.1%} ({lo:.1%}-{hi:.1%})   "
          f"confident: {ck:3d}/{cn:<3d} = {ck/max(cn,1):6.1%} ({clo:.1%}-{chi:.1%}), coverage {cn/max(n,1):.0%}")
line('Overall', df)
for ds, d in df.groupby('dataset'): line(ds, d)
print()
print(pd.crosstab([df.dataset, df.label], df.pred).to_string())
