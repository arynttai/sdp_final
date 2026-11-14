# Plain final report source

Run from the project root with Python 3:

```
python docs/report/generate_report.py
```

This folder is placed at `docs/report/` in the source submission. The generator
finds the project root automatically and writes `output/pdf/ITALIANO-Report.pdf`.
It reads short Java excerpts from the delivered source. `uml_helpers.py` draws
the six black-and-white class diagrams. Editable PlantUML sources are in `uml/`;
the three actual browser captures are in `assets/`.

Install the packages in `requirements.txt`. On Windows the report embeds Times
New Roman. Elsewhere it uses the standard PDF Times family. Body text is 12 pt
with 18 pt line spacing; headings are 14 pt bold. No application file is changed.
