# The Auris model

The model in `app/src/main/assets/` (`auris.onnx`, `labels.txt`, `model_info.json`) was made on 8 November 2026 by running `Auris_train_model.ipynb` on **public data only**. The notebook ran exactly as written, with only its Google Colab lines swapped for local paths (`run_locally.py`). It was trained on a CPU, not a GPU.

**Use it to try the app, not to diagnose patients.** It works well on images like the ones it was trained on and much worse on images from another hospital (see *Results by dataset*).

## Data

| Dataset | Images used | Licence |
|---|---|---|
| Eardrum database, Van Akdamar Hospital, Turkey: POLAT, KEMAL (2021). *eardrum.zip*. figshare. https://doi.org/10.6084/m9.figshare.13648166.v1 | 262 of 282 | CC BY 4.0: credit the dataset wherever the app or model is shared |
| Ear imagery database, Universidad de Chile: Viscaino, Michelle; Cheein, Fernando Auat (2020). *Ear imagery database*. figshare. https://doi.org/10.6084/m9.figshare.11886630.v1 | 811 of 880 | CC0 |

The Chilean dataset asks that its article is cited too: Viscaino, M., Maass, J. C., Delano, P. H., Torrente, M., Stott, C., & Auat Cheein, F. (2020). Computer-aided diagnosis of external and middle ear conditions: A machine learning approach. *PLOS ONE, 15*(3), e0229226. https://doi.org/10.1371/journal.pone.0229226

The notebook describes the Turkish link as 956 images. That figshare record holds 282. The 956-image version is the Kaggle dataset `erdalbasaran/eardrum-dataset-otitis-media`, which needs a Kaggle account and was not used.

| Condition | Chile | Turkey | Total |
|---|---|---|---|
| Normal | 195 | 154 | 349 |
| Wax | 212 | 21 | 233 |
| CSOM / perforation | 201 | 14 | 215 |
| Tympanosclerosis | 203 | 4 | 207 |
| AOM | 0 | 69 | 69 |

- **Duplicates.** 69 exact duplicates, all in the Chilean set, were kept once.
- **Left out.** Otitis externa (18 images, folder `OtitisEksterna`) and ventilation tubes (2 images, folder `TympanostomyTubes`). Their folder names do not match the notebook's list, and both have fewer than the notebook's 30-image minimum.
- **Fair split.** The notebook linked 591 near-identical pairs, mostly repeated video frames in the Chilean set. Linked images always stay together in training, validation or test.
- **Split sizes.** 765 images for training, 154 for validation and 154 for the final test.

## Results on the held-out test images

From the notebook, which also wrote these figures into `model_info.json`:

- **Accuracy:** 139 of 154 correct, 90.3% (likely range 84.6% to 94.0%). Balanced accuracy, where every condition counts equally, is 91.7%.
- **Confident mode:** 91 of 92 correct, 98.9% (likely range 94.1% to 99.8%), on 60% of test images.
  - No threshold reached 95% on the validation images, so the notebook fell back to its default threshold of 0.90.
  - **The 95% target is not met.** The bottom of the likely range is below 95%.
- **Training:** stopped early at epoch 16. The best epoch, 9, was kept (88.6% balanced accuracy on validation).

| Condition | Test images | Sensitivity | Specificity | Precision |
|---|---|---|---|---|
| AOM | 10 | 90% | 93% | 47% |
| CSOM / perforation | 31 | 97% | 100% | 100% |
| Normal | 49 | 78% | 98% | 95% |
| Tympanosclerosis | 30 | 100% | 98% | 91% |
| Wax | 34 | 94% | 100% | 100% |

The confusion matrix is in `results/confusion_matrix.png`.

## Results by dataset

`check_by_dataset.py` scores the exported `auris.onnx` with ONNX Runtime 1.27.0, the version the app uses. Full output is in `results/test_by_dataset.txt`.

| Test images from | Correct | When confident (≥ 0.90) |
|---|---|---|
| Chile | 118 of 121, 97.5% (93.0% to 99.2%) | 83 of 83, on 69% of images |
| Turkey | 21 of 33, 63.6% (46.6% to 77.8%) | 8 of 9, on 27% of images |

- **AOM false alarms.** 8 of the 20 normal Turkish test ears were called AOM. Fewer than half of all AOM answers were right.
- **Why.** Every AOM image came from the Turkish hospital. The model has partly learned the look of that hospital's images as a sign of AOM.
- **Images from new clinics.** Expect accuracy closer to the Turkish figure, or lower, until the model is tested there.

## Making it better

- **AOM.** Add ENT-confirmed AOM images from more than one clinic, and normal ears from the same clinics as each disease, so source can't stand in for diagnosis.
- **More conditions.** Add the conditions this model does not cover yet: OME, otitis externa, otomycosis, cholesteatoma, grommets and others.
- **New images.** Use the Kaggle and IEEE DataPort datasets listed in the notebook, plus images from partner clinics collected with consent and with names removed.
- **External test.** Test on images from a clinic that supplied none of the training data, and record doctors' verdicts in the app.

## Rebuilding the model

To use your own model, run the notebook in Colab with your images, then replace the three files in `app/src/main/assets/`. To repeat this run on a computer instead:

```
pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
pip install onnx onnxscript onnxruntime pandas scikit-learn matplotlib
python training/run_locally.py training/Auris_train_model.ipynb OUT_DIR WORK_DIR eardrum_folder ear_imagery_folder
python training/check_by_dataset.py OUT_DIR
```

This run used torch 2.15.0 (CPU), torchvision 0.29.2, onnx 1.23.2, onnxscript 0.7.2 and onnxruntime 1.31.1. It took about 12 minutes on 4 CPU cores. The export is ONNX opset 20 with a fixed `[1, 3, 224, 224]` input, and it also loads in ONNX Runtime 1.27.0.
