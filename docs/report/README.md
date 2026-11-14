# Final academic report

`generate_report.py` creates the English ITALIANO report and its editable UML assets.
It reads actual Java source excerpts, Git contribution metadata and Maven Surefire
results from this repository. It does not change application code.

The six class diagrams and integration sequence are saved as SVG and PlantUML
source under `uml/`. Their vector equivalents are embedded directly in the PDF.
The diagrams omit incidental DTO accessors while preserving pattern roles and
relationships.

Run with Python 3 and `reportlab`, `Pillow`, and `pypdf` installed:

```powershell
python docs/report/generate_report.py --screenshots-ready --demo-log docs/report/demo-output.txt
```

Python dependencies are listed in `requirements.txt`. The generator embeds the
Calibri fonts supplied with Windows; their font files are not redistributed.
This matches the Windows workstation used for the verified export.

The final output is `output/pdf/ITALIANO-Final-Report.pdf`. The screenshot files
are `docs/report/assets/dark-desktop-menu.png`, `light-desktop-dialog.png`, and
`dark-mobile-completed-order.png`; override them with the corresponding command-line options
if the final reviewed captures are elsewhere. Do not use `--screenshots-ready`
until they show the current English ITALIANO interface. `--draft` uses explicit
image placeholders and writes a temporary layout preview instead.

Before exporting a final report, run the project checks, save the successful
`--demo` terminal output to `demo-output.txt`, and refresh the screenshots. The
report reads test counts from the current `target/surefire-reports/TEST-*.xml`.
The report does not assert that a student has submitted to Moodle or completed
the oral defense.
