"""Build an evidence-based, editable academic PDF; no application mutations."""
from __future__ import annotations

import argparse
import html
import json
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image as PILImage
from pypdf import PdfReader
from reportlab import rl_config
from reportlab.graphics import renderSVG
from reportlab.graphics.shapes import Drawing, Group, Line, Polygon, PolyLine, Rect, String
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen.canvas import Canvas
from reportlab.platypus import (
    Flowable, Image, KeepTogether, PageBreak, Paragraph, Preformatted,
    SimpleDocTemplate, Spacer, Table, TableStyle,
)

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
JAVA = ROOT / "src/main/java/com/restaurant"
WIDTH = A4[0] - 84
INK = colors.HexColor("#25231f")
MUTED = colors.HexColor("#6c665d")
GOLD = colors.HexColor("#9a713e")
LINE = colors.HexColor("#ded7cc")
PALE = colors.HexColor("#f6f3ee")
rl_config.warnOnMissingFontGlyphs = 1


def register_fonts():
    windows = Path("C:/Windows/Fonts")
    fonts = {"Report": "calibri.ttf", "ReportBold": "calibrib.ttf", "ReportItalic": "calibrii.ttf"}
    for name, filename in fonts.items():
        font = windows / filename
        if not font.exists():
            raise RuntimeError(f"Required report font missing: {font}")
        pdfmetrics.registerFont(TTFont(name, str(font)))
    pdfmetrics.registerFontFamily("Report", normal="Report", bold="ReportBold", italic="ReportItalic", boldItalic="ReportBold")


register_fonts()
STYLES = {
    "Body": ParagraphStyle("Body", fontName="Report", fontSize=10, leading=13.8, textColor=INK, spaceAfter=8),
    "Small": ParagraphStyle("Small", fontName="Report", fontSize=8.4, leading=11, textColor=MUTED, spaceAfter=6),
    "Caption": ParagraphStyle("Caption", fontName="Report", fontSize=8, leading=10.5, textColor=MUTED, spaceAfter=10),
    "Title": ParagraphStyle("Title", fontName="ReportBold", fontSize=25, leading=29, textColor=INK, spaceAfter=14),
    "Sub": ParagraphStyle("Sub", fontName="ReportBold", fontSize=12, leading=15, textColor=INK, spaceBefore=9, spaceAfter=7),
    "Kicker": ParagraphStyle("Kicker", fontName="ReportBold", fontSize=8, leading=12, textColor=GOLD, spaceAfter=8, tracking=1.1),
    "Cell": ParagraphStyle("Cell", fontName="Report", fontSize=8.4, leading=11.2, textColor=INK),
    "CellSmall": ParagraphStyle("CellSmall", fontName="Report", fontSize=7.8, leading=10.2, textColor=INK),
    "Cover": ParagraphStyle("Cover", fontName="ReportBold", fontSize=46, leading=51, textColor=INK, spaceAfter=18),
    "CoverSub": ParagraphStyle("CoverSub", fontName="Report", fontSize=20, leading=25, textColor=MUTED, spaceAfter=18),
}


def p(text, style="Body"):
    return Paragraph(text, STYLES[style])


def title(section, text):
    return [p(section.upper(), "Kicker"), p(text, "Title")]


def table(headers, rows, widths, small=False):
    style = "CellSmall" if small else "Cell"
    data = [[p(f"<b>{html.escape(str(c))}</b>", style) for c in headers]]
    data.extend([[p(str(c), style) for c in row] for row in rows])
    result = Table(data, colWidths=widths, hAlign="LEFT", repeatRows=1)
    result.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), PALE),
        ("LINEBELOW", (0, 0), (-1, 0), .65, LINE),
        ("LINEBELOW", (0, 1), (-1, -1), .35, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 8),
        ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, -1), 7),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
    ]))
    return result


def code(relative, start_text, end_text=None, count=None, size=7.1):
    path = JAVA / relative
    lines = path.read_text(encoding="utf-8").splitlines()
    first = next(i for i, line in enumerate(lines) if start_text in line)
    if end_text is not None:
        last = next(i for i in range(first, len(lines)) if end_text in lines[i]) + 1
    else:
        last = min(first + (count or 8), len(lines))
    selected = lines[first:last]
    indent = min(len(line) - len(line.lstrip()) for line in selected if line.strip())
    selected = [line[indent:] for line in selected]
    formatted = "\n".join(f"{first+i+1:>3}  {line}" for i, line in enumerate(selected))
    style = ParagraphStyle("Source", fontName="Courier", fontSize=size, leading=size * 1.3, textColor=INK)
    block = Table([[Preformatted(formatted, style)]], colWidths=[WIDTH])
    block.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), PALE),
        ("BOX", (0, 0), (-1, -1), .45, LINE),
        ("LEFTPADDING", (0, 0), (-1, -1), 9),
        ("RIGHTPADDING", (0, 0), (-1, -1), 9),
        ("TOPPADDING", (0, 0), (-1, -1), 8),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
    ]))
    label = f"Source: src/main/java/com/restaurant/{relative}, lines {first+1}-{last}."
    return [block, Spacer(1, 5), p(label, "Caption")]


def class_box(d, x, y, w, h, name, members=(), stereotype=None):
    d.add(Rect(x, y, w, h, fillColor=colors.white, strokeColor=GOLD, strokeWidth=.75))
    names = name.split("\n")
    top = y + h - 12
    if stereotype:
        d.add(String(x + w/2, top, f"<<{stereotype}>>", fontName="ReportItalic", fontSize=6.9, textAnchor="middle", fillColor=MUTED))
        top -= 10
    for line in names:
        d.add(String(x + w/2, top, line, fontName="ReportBold", fontSize=8, textAnchor="middle", fillColor=INK))
        top -= 10
    if members:
        divider = top + 3
        d.add(Line(x, divider, x+w, divider, strokeColor=LINE, strokeWidth=.5))
        for member in members:
            top -= 8
            d.add(String(x+7, top, member, fontName="Report", fontSize=6.8, fillColor=INK))


def arrow(d, points, kind="arrow", label=None, label_at=None, dashed=False):
    d.add(PolyLine(points, strokeColor=MUTED, strokeWidth=.65, fillColor=None, strokeDashArray=[3,2] if dashed else None))
    x1,y1 = points[-2]; x2,y2 = points[-1]
    dx,dy = x2-x1,y2-y1
    norm = (dx*dx+dy*dy)**.5 or 1
    ux,uy = dx/norm,dy/norm
    bx,by = x2-ux*7,y2-uy*7
    if kind in ("inherit", "implements"):
        d.add(Polygon([x2,y2,bx-uy*3.3,by+ux*3.3,bx+uy*3.3,by-ux*3.3], fillColor=colors.white, strokeColor=MUTED, strokeWidth=.65))
    elif kind == "arrow":
        d.add(Line(x2,y2,bx-uy*2.8,by+ux*2.8,strokeColor=MUTED,strokeWidth=.65))
        d.add(Line(x2,y2,bx+uy*2.8,by-ux*2.8,strokeColor=MUTED,strokeWidth=.65))
    if label:
        lx,ly = label_at or ((x1+x2)/2, (y1+y2)/2 + 4)
        d.add(String(lx,ly,label,fontName="Report",fontSize=6.6,textAnchor="middle",fillColor=MUTED))


def diagrams():
    result = {}
    d = Drawing(500,300)
    class_box(d,20,228,166,64,"MealFactory",["+create(definition): Meal", "#createMeal(definition): Meal"],"abstract")
    class_box(d,360,228,140,64,"Meal",["+price(): BigDecimal", "+id(); +name(); +category()"],"interface")
    class_box(d,360,136,140,57,"BaseMeal",["-definition: MenuDefinition"],"abstract")
    arrow(d,[(430,193),(430,228)],"implements",dashed=True)
    for x,creator,product in [(0,"PizzaFactory","Pizza"),(116,"PastaFactory","Pasta"),(232,"DrinkFactory","Drink")]:
        class_box(d,x,137,105,55,creator,["#createMeal(...): Meal"])
        class_box(d,x,15,105,45,product)
        arrow(d,[(x+52,192),(x+52,210),(103,210),(103,228)],"inherit")
        arrow(d,[(x+52,137),(x+52,60)],label="creates",label_at=(x+75,99),dashed=True)
        arrow(d,[(x+105,38),(345,38),(345,115),(430,115),(430,136)],"inherit")
    result["factory"] = d
    d = Drawing(500,248)
    class_box(d,0,88,195,145,"Order.Builder",["-items: List<OrderLine>","+customer(name): Builder","+fulfillment(mode): Builder","+tableNumber(n): Builder","+addItem(line): Builder","+build(): Order"])
    class_box(d,300,94,200,138,"Order",["-id: UUID; -customerName","-fulfillment; -tableNumber","-items: List<OrderLine>","-subtotal: BigDecimal","-Order(builder)","+items(); +subtotal()"],"immutable")
    class_box(d,300,0,200,61,"OrderLine",["+meal(): Meal; +quantity()","+lineTotal(): BigDecimal"],"record")
    class_box(d,0,0,195,61,"Meal",["+price(): BigDecimal"],"interface")
    arrow(d,[(195,173),(300,173)],label="builds",dashed=True)
    arrow(d,[(400,94),(400,61)],label="contains 1..20",label_at=(450,77))
    arrow(d,[(300,30),(195,30)],label="meal",label_at=(247,35))
    result["builder"] = d
    d = Drawing(500,283)
    class_box(d,175,210,170,65,"Meal",["+name(): String", "+price(): BigDecimal"],"interface")
    class_box(d,0,120,155,61,"BaseMeal",["-definition"],"abstract")
    class_box(d,290,120,195,75,"ToppingDecorator",["-wrapped: Meal; -topping","+name(); +price()"],"abstract")
    arrow(d,[(77,181),(77,240),(175,240)],"implements",dashed=True)
    arrow(d,[(387,195),(387,240),(345,240)],"implements",dashed=True)
    arrow(d,[(485,160),(498,160),(498,265),(345,265)],label="wraps Meal",label_at=(430,269))
    for x,name in [(0,"ExtraCheese"),(175,"Mushrooms"),(350,"Olives")]:
        class_box(d,x,12,145,58,name,["+constructor(Meal)"])
        arrow(d,[(x+72,70),(x+72,99),(387,99),(387,120)],"inherit")
    result["decorator"] = d
    d = Drawing(500,264)
    class_box(d,0,170,220,87,"ReceiptDocument",["-renderer: ReceiptRenderer","+render(order, pricing): String","#content(order, pricing)"],"abstract")
    class_box(d,305,170,195,87,"ReceiptRenderer",["+render(ReceiptContent): String"],"interface")
    arrow(d,[(220,213),(305,213)],label="delegates formatting",label_at=(262,219))
    for x,name in [(0,"CustomerReceipt"),(146,"KitchenTicket")]:
        class_box(d,x,35,134,77,name,["#content(order, pricing)"])
        arrow(d,[(x+67,112),(x+67,145),(110,145),(110,170)],"inherit")
    for x,name in [(310,"TextReceipt\nRenderer"),(410,"HtmlReceipt\nRenderer")]:
        class_box(d,x,35,90,77,name,["+render(content)"])
        arrow(d,[(x+45,112),(x+45,145),(402,145),(402,170)],"implements",dashed=True)
    result["bridge"] = d
    d = Drawing(500,258)
    class_box(d,175,182,170,68,"DiscountStrategy",["+code(): String","+discount(Order): BigDecimal"],"interface")
    class_box(d,0,77,184,80,"PricingCalculator",["+calculate(order, strategy)","  : Pricing"])
    class_box(d,0,0,184,60,"Pricing",["+subtotal(); +discount(); +total()"],"record")
    arrow(d,[(92,157),(92,216),(175,216)],label="uses",label_at=(122,222))
    arrow(d,[(92,77),(92,60)],dashed=True)
    for x,name,rule in [(195,"NoDiscount","0%"),(300,"StudentDiscount","10%"),(405,"PickupDiscount","20%, takeaway")]:
        class_box(d,x,65,95,80,name.replace("Discount","\nDiscount"),["+discount(order)",rule])
        arrow(d,[(x+47,145),(x+47,166),(260,166),(260,182)],"implements",dashed=True)
    result["strategy"] = d
    d = Drawing(500,285)
    class_box(d,0,138,220,140,"OrderProgress",["-orderId: UUID; -status","-observers: Set<OrderObserver>","+subscribe(); +unsubscribe()","+announce(); +advance()","+transitionTo(next)","+notifications(): List"])
    class_box(d,305,209,195,69,"OrderObserver",["+onStatusChanged(change)","  : OrderNotification"],"interface")
    arrow(d,[(220,245),(305,245)],label="notifies subscribers",label_at=(262,252))
    for x,name in [(270,"Customer\nObserver"),(390,"Kitchen\nObserver")]:
        class_box(d,x,94,110,73,name,["+onStatusChanged(...)"])
        arrow(d,[(x+55,167),(x+55,188),(402,188),(402,209)],"implements",dashed=True)
    class_box(d,0,10,220,90,"StatusChange",["orderId: UUID; status: OrderStatus","occurredAt: Instant"],"record")
    class_box(d,270,10,230,60,"OrderNotification",["orderId; status; audience; message; time"],"record")
    arrow(d,[(110,138),(110,100)],label="publishes",label_at=(155,116),dashed=True)
    arrow(d,[(325,94),(325,70)],dashed=True)
    arrow(d,[(445,94),(445,70)],dashed=True)
    result["observer"] = d
    d = Drawing(500,485)
    actors=[("Browser",20),("Order\nService",85),("Menu\nCatalog",150),("Topping\nCatalog",215),("Order.\nBuilder",280),("Pricing\nCalculator",345),("Order\nProgress",410),("Receipt\nDocument",475)]
    for name,x in actors:
        d.add(Rect(x-25,440,50,40,fillColor=PALE,strokeColor=GOLD,strokeWidth=.6))
        for j,line in enumerate(name.split("\n")):
            d.add(String(x,465-j*10,line,fontName="ReportBold",fontSize=6.6,textAnchor="middle",fillColor=INK))
        d.add(Line(x,10,x,440,strokeColor=LINE,strokeWidth=.6,strokeDashArray=[3,3]))
    steps=[
        (20,85,"place(request)",False),
        (85,150,"create(menuItemId)",False),
        (150,85,"Meal",True),
        (85,215,"decorate(meal, toppings)",False),
        (215,85,"decorated Meal",True),
        (85,280,"addItem(...), build()",False),
        (280,85,"immutable Order",True),
        (85,345,"calculate(order, strategy)",False),
        (345,85,"Pricing: subtotal, discount, total",True),
        (85,410,"subscribe customer + kitchen",False),
        (85,410,"announce(): publish PLACED to both",False),
        (410,85,"notifications()",True),
        (85,20,"OrderView",True),
        (20,85,"advance(orderId) [demo action]",False),
        (85,410,"advance(): next valid status, notify",False),
        (85,20,"updated OrderView",True),
        (20,85,"receipt(orderId, format, type)",False),
        (85,475,"render(order, pricing) via renderer",False),
        (475,85,"HTML or text document",True),
        (85,20,"receipt response",True),
    ]
    for i,(a,b,label,is_return) in enumerate(steps):
        y=421-i*20
        arrow(d,[(a,y),(b,y)],label=label,label_at=((a+b)/2,y+3),dashed=is_return)
    result["sequence"] = d
    return result


UML_SOURCES = {
"factory": """abstract class MealFactory { +create(MenuDefinition): Meal\n# createMeal(MenuDefinition): Meal }
class PizzaFactory\nclass PastaFactory\nclass DrinkFactory
interface Meal\nabstract class BaseMeal\nclass Pizza\nclass Pasta\nclass Drink\nclass MenuCatalog
Meal <|.. BaseMeal\nBaseMeal <|-- Pizza\nBaseMeal <|-- Pasta\nBaseMeal <|-- Drink
MealFactory <|-- PizzaFactory\nMealFactory <|-- PastaFactory\nMealFactory <|-- DrinkFactory
PizzaFactory ..> Pizza : creates\nPastaFactory ..> Pasta : creates\nDrinkFactory ..> Drink : creates
MenuCatalog --> MealFactory : selects by category
""",
"builder": """class \"Order.Builder\" as Builder { +customer(name): Builder\n+fulfillment(mode): Builder\n+addItem(line): Builder\n+build(): Order }
class Order <<immutable>>\nclass OrderLine <<record>>\ninterface Meal
Builder ..> Order : constructs\nOrder *-- \"1..20\" OrderLine\nOrderLine --> Meal
""",
"decorator": """interface Meal\nabstract class BaseMeal\nabstract class ToppingDecorator { -wrapped: Meal\n+price(): BigDecimal }
class ExtraCheese\nclass Mushrooms\nclass Olives\nclass ToppingCatalog
Meal <|.. BaseMeal\nMeal <|.. ToppingDecorator\nToppingDecorator o-- Meal : wraps
ToppingDecorator <|-- ExtraCheese\nToppingDecorator <|-- Mushrooms\nToppingDecorator <|-- Olives
ToppingCatalog ..> ToppingDecorator : constructs selected wrappers
""",
"bridge": """abstract class ReceiptDocument { -renderer: ReceiptRenderer\n+render(Order,Pricing): String\n#content(Order,Pricing): ReceiptContent }
class CustomerReceipt\nclass KitchenTicket\ninterface ReceiptRenderer\nclass TextReceiptRenderer\nclass HtmlReceiptRenderer\nclass ReceiptContent <<record>>
ReceiptDocument <|-- CustomerReceipt\nReceiptDocument <|-- KitchenTicket\nReceiptDocument o-- ReceiptRenderer
ReceiptRenderer <|.. TextReceiptRenderer\nReceiptRenderer <|.. HtmlReceiptRenderer\nReceiptDocument ..> ReceiptContent
""",
"strategy": """interface DiscountStrategy { +code(): String\n+discount(Order): BigDecimal }
class NoDiscount\nclass StudentDiscount\nclass PickupDiscount\nclass PricingCalculator\nclass Pricing <<record>>
DiscountStrategy <|.. NoDiscount\nDiscountStrategy <|.. StudentDiscount\nDiscountStrategy <|.. PickupDiscount
PricingCalculator --> DiscountStrategy : calculates one discount\nPricingCalculator ..> Pricing : returns
""",
"observer": """interface OrderObserver { +onStatusChanged(StatusChange): OrderNotification }
class OrderProgress { +subscribe(OrderObserver)\n+unsubscribe(OrderObserver)\n+announce()\n+advance()\n+transitionTo(OrderStatus) }
class CustomerObserver\nclass KitchenObserver\nclass StatusChange <<record>>\nclass OrderNotification <<record>>
OrderObserver <|.. CustomerObserver\nOrderObserver <|.. KitchenObserver\nOrderProgress o-- OrderObserver : subscribers
OrderProgress ..> StatusChange : publishes\nOrderObserver ..> OrderNotification : returns
""",
"sequence": """actor Browser\nparticipant OrderService\nparticipant MenuCatalog\nparticipant ToppingCatalog\nparticipant \"Order.Builder\" as Builder\nparticipant PricingCalculator\nparticipant OrderProgress\nparticipant ReceiptDocument
Browser -> OrderService: place(request)
OrderService -> MenuCatalog: create(menuItemId)\nMenuCatalog --> OrderService: Meal
OrderService -> ToppingCatalog: decorate(Meal, toppingIds)\nToppingCatalog --> OrderService: decorated Meal
OrderService -> Builder: addItem(OrderLine), build()\nBuilder --> OrderService: immutable Order
OrderService -> PricingCalculator: calculate(Order, DiscountStrategy)\nPricingCalculator --> OrderService: Pricing
OrderService -> OrderProgress: subscribe(CustomerObserver), subscribe(KitchenObserver)
OrderService -> OrderProgress: announce(): PLACED notifications\nOrderService --> Browser: OrderView
Browser -> OrderService: advance(orderId) [demo]\nOrderService -> OrderProgress: advance(): notify next status\nOrderService --> Browser: updated OrderView
Browser -> OrderService: receipt(orderId, format, type)\nOrderService -> ReceiptDocument: render(Order,Pricing)\nReceiptDocument --> OrderService: formatted document\nOrderService --> Browser: receipt response
""",
}


def export_uml(drawings):
    directory = HERE / "uml"
    directory.mkdir(parents=True, exist_ok=True)
    for name,drawing in drawings.items():
        renderSVG.drawToFile(drawing,str(directory / f"{name}.svg"))
        source = UML_SOURCES[name].replace("{ ", "{\n  ").replace(" }", "\n}")
        (directory / f"{name}.puml").write_text("@startuml\nskinparam classAttributeIconSize 0\n" + source + "@enduml\n",encoding="utf-8")


def scaled(drawing, width=WIDTH, height=None):
    factor = min(width/drawing.width, height/drawing.height if height else 1)
    if factor == 1:
        return drawing
    holder = Drawing(drawing.width*factor,drawing.height*factor)
    group = Group(drawing)
    group.scale(factor,factor)
    holder.add(group)
    return holder


def test_evidence():
    suites=[]
    for file in sorted((ROOT / "target/surefire-reports").glob("TEST-*.xml")):
        element=ET.parse(file).getroot()
        suites.append({"name":element.attrib["name"].split(".")[-1], **{k:int(element.attrib.get(k,0)) for k in ("tests","failures","errors","skipped")}})
    if not suites:
        raise RuntimeError("No Maven Surefire test evidence exists. Run verify first.")
    totals={k:sum(s[k] for s in suites) for k in ("tests","failures","errors","skipped")}
    if totals["failures"] or totals["errors"]:
        raise RuntimeError(f"Cannot report successful verification: {totals}")
    return suites,totals


def commit_info(sha):
    output=subprocess.check_output(["git","show","-s","--format=%h|%an|%ae|%s",sha],cwd=ROOT,text=True,encoding="utf-8").strip()
    return output.split("|",3)


class Placeholder(Flowable):
    def __init__(self,w,h,label):
        super().__init__(); self.width=w; self.height=h; self.label=label
    def draw(self):
        self.canv.setFillColor(PALE); self.canv.setStrokeColor(LINE)
        self.canv.rect(0,0,self.width,self.height,fill=1,stroke=1)
        self.canv.setFont("Report",11); self.canv.setFillColor(MUTED)
        self.canv.drawCentredString(self.width/2,self.height/2,self.label)


def photo(path, max_w, max_h, draft=False, label="Screenshot awaiting final QA"):
    if draft:
        return Placeholder(max_w,max_h,label)
    if not path.exists():
        raise RuntimeError(f"Reviewed screenshot missing: {path}")
    with PILImage.open(path) as im:
        w,h=im.size
    scale=min(max_w/w,max_h/h)
    return Image(str(path),w*scale,h*scale,hAlign="CENTER")


class NumberedCanvas(Canvas):
    def __init__(self,*args,**kwargs):
        super().__init__(*args,**kwargs); self.saved=[]
    def showPage(self):
        self.saved.append(dict(self.__dict__)); self._startPage()
    def save(self):
        total=len(self.saved)
        for state in self.saved:
            self.__dict__.update(state)
            if self._pageNumber>1:
                self.setStrokeColor(LINE); self.setLineWidth(.45)
                self.line(42,A4[1]-34,A4[0]-42,A4[1]-34)
                self.setFont("Report",8); self.setFillColor(MUTED)
                self.drawString(42,A4[1]-26,"ITALIANO  /  Software Design Patterns")
                self.drawRightString(A4[0]-42,A4[1]-26,"Final project report")
            self.setFont("Report",8); self.setFillColor(MUTED)
            self.drawString(42,24,"Anuar  /  Akmaral  /  Mukhammad")
            self.drawRightString(A4[0]-42,24,f"{self._pageNumber:02d} / {total:02d}")
            Canvas.showPage(self)
        Canvas.save(self)


def build(args):
    drawings=diagrams(); export_uml(drawings)
    suites,totals=test_evidence()
    story=[]
    def add(*items): story.extend(items)
    def page(): add(PageBreak())
    def bullets(items):
        for text in items: add(p("- " + text))

    # 1. Cover and reader map.
    add(Spacer(1,56),p("FINAL PROJECT  /  14 NOVEMBER 2026","Kicker"),p("ITALIANO","Cover"),
        p("Restaurant Ordering System","CoverSub"),p("Six coordinated design patterns in Java 21 and Spring Boot.","Body"),Spacer(1,19))
    add(table(["Team member","Individual pattern implementation"],[
        ["Anuar","Factory Method and Builder"],["Akmaral","Strategy and Bridge"],["Mukhammad","Decorator and Observer; website integration"]],[135,WIDTH-135]))
    add(Spacer(1,21),p("Project summary","Sub"),p("ITALIANO turns meal selection, optional toppings, order construction, discounts, kitchen/customer notifications and receipt rendering into one tested workflow. The design keeps product construction, customization, pricing, progress and document output behind separate contracts."))
    add(p("The restaurant interface provides English ordering controls and reviewed light/dark presentation themes. The application remains an educational demonstration: orders are stored per HTTP session; no payment or external restaurant messaging takes place."))
    add(Spacer(1,13),p("Reading guide","Sub"),table(["Pages","Evidence"],[
        ["2-4","Requirements, architecture and real team contribution history"],["5-10","Six class diagrams, design choices and sourced Java excerpts"],["11-13","Integrated sequence, clean-code reasoning and verification"],["14-16","Application screenshots, conclusion and further work"]],[62,WIDTH-62]))
    add(Spacer(1,10),p("Repository: github.com/arynttai/sdp_final. This report documents completed code and verifiable artifacts; personal Moodle uploads and the live oral defense remain each student's responsibility.","Small"))
    page()

    # 2. Intro and complete criteria matrix.
    add(*title("01 / Introduction","Purpose and assignment criteria"))
    add(p("A restaurant order contains independent choices: meal type, optional toppings, service mode, pricing rule and output document. A single conditional-heavy implementation would couple these choices. This project assigns each variation to an appropriate pattern, then coordinates them through one application service."))
    add(p("The goal is to demonstrate six operational patterns, with two implemented by each student, while retaining a usable website and a repeatable console demonstration. The required report structure is Introduction, Body, Conclusion and Further Work, with code, screenshots and UML."))
    add(table(["Criterion","Evidence and status"],[
        ["At least six distinct integrated patterns","Met: Factory Method, Builder, Decorator, Bridge, Strategy and Observer (pp. 5-11)."],
        ["At least two patterns per student","Met: contribution assignments and original implementation commits (p. 4)."],
        ["Creational, structural and behavioral coverage","Met: two patterns in each category (pp. 5-10)."],
        ["Demonstrably operational workflow","Automated domain/API tests and an assertion-based --demo; results on p. 13."],
        ["Six pattern packages; only Main at root","Met in production Java sources; PackageLayoutTest validates the layout (p. 3)."],
        ["SOLID, KISS, DRY and OOP","Concrete interface, composition, validation and reuse examples (p. 12)."],
        ["Git history from every team member","Original authors and implementation commits are retained (p. 4)."],
        ["Report sections, Java code, screenshots, UML","Provided throughout this report; screenshots on pp. 14-15."],
        ["PDF report and zipped source submission","This PDF is the report artifact; source ZIP is a separate deliverable."],
        ["Each student uploads to Moodle; oral defense","Individual actions required. This report does not claim either is completed."],
    ],[153,WIDTH-153],small=True))
    add(Spacer(1,10),p("Assessment context supplied with the task: code and Git 50%, report 10%, oral defense 40%. These percentages describe the course rubric, not an estimated project grade.","Small"))
    page()

    # 3. Architecture and packages.
    add(*title("02 / Body - architecture","One application, six pattern packages"))
    add(p("The browser and console demonstration use the same Java pattern contracts. HTTP adapters translate validated requests into the application service; they do not calculate authoritative prices in JavaScript. The server returns menu metadata, quotes, orders, status history and receipts."))
    add(table(["Production package","Role subfolders and responsibility"],[
        ["factory","creator, product, catalog: validate definitions and construct Meal objects."],
        ["builder","product, application, repository, web: immutable orders, orchestration, session book and HTTP adapters."],
        ["decorator","component, concrete, catalog: composable topping wrappers and validated selection metadata."],
        ["strategy","api, concrete, context: interchangeable discounts and validated Pricing values."],
        ["observer","api, concrete, event, subject: subscriptions, valid progress transitions and notifications."],
        ["bridge","abstraction, implementation: document purpose independent of rendering format."],
    ],[103,WIDTH-103]))
    add(Spacer(1,12),p("Coordination boundary","Sub"),p("OrderService builds a Meal through MenuCatalog, wraps it through ToppingCatalog, adds an OrderLine to Order.Builder, applies PricingCalculator, subscribes the customer/kitchen observers and stores the result. A later receipt request selects a document and renderer independently."))
    add(p("Only Main.java sits directly in com.restaurant. The required six package roots remain explicit; web infrastructure is placed under builder to preserve that course layout. Domain contracts such as Meal, DiscountStrategy and OrderObserver do not depend on HTTP."))
    add(p("Data and lifecycle constraints","Sub"))
    bullets(["Order and pricing data use immutable values and BigDecimal. OrderProgress owns mutable status separately.","Each HTTP session receives an OrderBook limited to 20 orders. Session expiration or process restart removes its orders.","Repeated placement with the same request ID and same payload returns the existing order; conflicting reuse is rejected.","The static browser assets, menu photographs, fonts and theme logos are served locally. Themes change presentation, not pattern behavior."])
    page()

    # 4. Contribution evidence.
    add(*title("03 / Body - collaboration","Individual work and Git evidence"))
    add(p("The repository preserves separate implementation commits by all three members. The assignment mapping below is supported by the actual commit author metadata, not by profile contribution graphs or merge ownership."))
    rows=[]
    for who,pattern,sha in [("Anuar","Factory Method","cd79d48"),("Anuar","Builder","2792aba"),("Akmaral","Strategy","f05a62e"),("Akmaral","Bridge","b78f7d6"),("Mukhammad","Decorator","8c41231"),("Mukhammad","Observer","f70398d")]:
        short,author,email,subject=commit_info(sha)
        rows.append([who,pattern,f"<b>{short}</b><br/>{html.escape(subject)}<br/><font color='#6c665d'>Author: {html.escape(author)}</font>"])
    add(table(["Student","Pattern","Original implementation commit"],rows,[68,91,WIDTH-159],small=True))
    add(Spacer(1,13),p("Integration contributions","Sub"),p("Mukhammad's 2a12537 connects the six patterns to the website and retains the earlier team contracts. Subsequent UI and brand commits refine presentation. Anuar's creator/product and immutable order foundation and Akmaral's discount/document implementations remain visible in the shared history."))
    add(p("Verified identities","Sub"),table(["Student / Git author","GitHub-linked identity recorded in commits"],[
        ["Anuar / ANAYSSA","48955674+ANAYSSA@users.noreply.github.com"],
        ["Akmaral / Akmaral","136558349+arynttai@users.noreply.github.com"],
        ["Mukhammad / Mukhammad","muhanm0707@gmail.com"],
    ],[155,WIDTH-155],small=True))
    add(Spacer(1,10),p("Merge commits authored as vertex use Mukhammad's email; they are integration records, not an additional student. No authorship is reassigned. The baseline inspected for this report is b5546ac, plus the current English ITALIANO interface revision.","Small"))
    page()

    # 5-10. Pattern details and accurate source excerpts.
    patterns=[
        ("factory","04 / Body - creational pattern","Factory Method","Anuar",
         "Meal construction varies by category. The abstract creator keeps common validation and delegates the actual product construction to an overridden factory method.",
         [("Creator","MealFactory.create delegates to protected createMeal."),("Concrete creators","PizzaFactory, PastaFactory and DrinkFactory."),("Products","Meal interface; BaseMeal shared state; Pizza, Pasta and Drink concrete products."),("Client/catalog","MenuCatalog chooses a creator for a validated MenuDefinition.")],
         "factory/creator/MealFactory.java","public final Meal create",7,
         "This is Factory Method: subclasses override creation. It is not claimed as Abstract Factory. The diagram includes every concrete creator/product pair; the catalog is the selecting client.",
         "MealFactoryTest verifies distinct product types, values, definitions and rejection of unknown or incompatible products."),
        ("builder","05 / Body - creational pattern","Builder","Anuar",
         "Orders combine optional preparation notes and service-specific details with a variable list of meal lines. A fluent builder avoids a long public constructor and makes validation a single construction boundary.",
         [("Builder","Order.Builder, a public static nested class."),("Product","Order, immutable with a private constructor."),("Supporting values","OrderLine and Fulfillment."),("Clients","Main and OrderService assemble dine-in or takeaway configurations.")],
         "builder/product/Order.java","public Builder customer(String name)",9,
         "The nested builder deliberately remains inside Order's product file. build() checks customer length, fulfillment/table consistency, note limits and 1-20 lines; Order copies the item list.",
         "OrderBuilderTest covers valid configurations, validation and immutability; API tests exercise the same construction path."),
        ("decorator","06 / Body - structural pattern","Decorator","Mukhammad",
         "Toppings add behavior to an existing Meal at runtime. Each wrapper preserves the Meal contract, so factories, order lines and pricing can use base or decorated meals without special combination subclasses.",
         [("Component","factory.product.Meal."),("Concrete component","Pizza/Pasta via BaseMeal."),("Decorator","ToppingDecorator implements Meal and holds another Meal."),("Concrete decorators","ExtraCheese (+350), Mushrooms (+300), Olives (+300 KZT).")],
         "decorator/component/ToppingDecorator.java","public final String id()",7,
         "A wrapper can wrap another wrapper. ID and category are delegated; name, description and price are augmented. The constructor rejects drinks; ToppingCatalog validates up to three distinct known toppings.",
         "ToppingDecoratorTest checks nested composition, unchanged products, identity/category, invalid choices and integrated pricing."),
        ("bridge","07 / Body - structural pattern","Bridge","Akmaral",
         "Document purpose and output format vary independently. A customer receipt and a kitchen ticket can each be rendered as HTML or text, avoiding a separate subclass for every type/format combination.",
         [("Abstraction","ReceiptDocument."),("Refined abstractions","CustomerReceipt and KitchenTicket."),("Implementor","ReceiptRenderer interface."),("Concrete implementors","TextReceiptRenderer and HtmlReceiptRenderer; ReceiptContent carries values.")],
         "bridge/abstraction/ReceiptDocument.java","public final String render",5,
         "The document prepares content; the injected renderer formats it. Customer content includes discounted totals. Kitchen content excludes prices. HTML dynamic values are escaped, while text remains plain output.",
         "ReceiptTest covers all four combinations, totals, kitchen details, escaping and immutable receipt values."),
        ("strategy","08 / Body - behavioral pattern","Strategy","Akmaral",
         "Discount eligibility and calculations are interchangeable algorithms. PricingCalculator uses the selected DiscountStrategy once and returns a separate immutable Pricing result without mutating the order.",
         [("Strategy","DiscountStrategy.code and discount(Order)."),("Concrete strategies","NoDiscount, StudentDiscount (10%), PickupDiscount (20% for takeaway)."),("Context","PricingCalculator."),("Result","Pricing(subtotal, discount, total), a record.")],
         "strategy/context/PricingCalculator.java","BigDecimal subtotal =",8,
         "Strategies return the discount amount. The calculator rejects negative/excessive discounts, rounds to two decimals with HALF_UP and subtracts once. Order.total retains its original pre-discount meaning; Pricing.total is payable.",
         "PricingCalculatorTest checks each algorithm, takeaway eligibility, rounding, invalid amounts and one strategy call."),
        ("observer","09 / Body - behavioral pattern","Observer","Mukhammad",
         "Kitchen and customer messages respond to the same order progress event. The subject depends on a small observer interface and does not contain audience-specific message construction.",
         [("Subject","OrderProgress holds subscriptions, status and history."),("Observer contract","OrderObserver.onStatusChanged(StatusChange)."),("Concrete observers","CustomerObserver and KitchenObserver."),("Event/results","StatusChange and OrderNotification records.")],
         "observer/subject/OrderProgress.java","private void publish()",6,
         "Observers are subscribed before PLACED is announced. Valid progress is PLACED, PREPARING, READY, COMPLETED. Repeating the current status is idempotent; skipped/backward changes are rejected. Publication is synchronous in Java, not a claim of external push delivery.",
         "OrderProgressTest checks both audiences, transitions, duplicate subscriptions, unsubscribe, snapshots and independent subjects."),
    ]
    for key,section,name,author,intro,roles,relative,start,count,note,verification in patterns:
        add(*title(section,name),p(f"<b>Implemented by {author}.</b> {intro}"))
        add(table(["Pattern role","Actual implementation"],roles,[111,WIDTH-111],small=True),Spacer(1,9))
        add(scaled(drawings[key],height=245),Spacer(1,5),p(f"Figure {list(drawings).index(key)+1}. {name} class relationships. Hollow triangles denote inheritance or interface realization; dashed dependencies indicate creation/use.","Caption"))
        add(*code(relative,start,count=count,size=6.9))
        add(p(note,"Small"),p("<b>Verification:</b> "+verification,"Small"))
        page()

    # 11. Integrated UML sequence and arithmetic.
    add(*title("10 / Body - integration","One order across all six patterns"))
    add(p("The sequence follows OrderService.place/build, progress operations and receipt generation. A diagram arrow is an application call or returned value; event publication invokes the two Java observers inside OrderProgress.","Small"))
    add(scaled(drawings["sequence"],height=430),p("Figure 7. Integrated order placement, demo progress and receipt sequence. Recipe construction, pricing and documents use the same domain values.","Caption"))
    add(table(["Worked example","Expected result"],[
        ["Two Margheritas, each with ExtraCheese","2 x (2000 + 350) = 4700 KZT subtotal"],
        ["STUDENT strategy","470 KZT discount; payable total 4230 KZT"],
        ["PLACED through COMPLETED","Four statuses x two subscribers = eight notifications"],
        ["Bridge receipt","Customer document reports the same 4230 KZT total"],
    ],[198,WIDTH-198],small=True))
    add(Spacer(1,8),p("Quote and placement reuse the same build-and-price path. The server's Pricing.total remains authoritative; formatting or changing the browser theme does not alter it.","Small"))
    page()

    # 12. OOP and clean principles.
    add(*title("11 / Body - software quality","OOP, SOLID, KISS and DRY"))
    add(p("The implementation uses patterns where behavior actually varies. The following examples explain the design decisions rather than treating the presence of interfaces as automatic proof of clean code."))
    add(table(["Principle","Concrete evidence and practical effect"],[
        ["Encapsulation / immutability","Order has private state, a private constructor and copied items; Pricing and event records expose values without mutable setters."],
        ["Inheritance / polymorphism","MealFactory subclasses override createMeal; Meal and observer/renderer/strategy interfaces allow substitution through contracts."],
        ["Composition","ToppingDecorator wraps Meal; ReceiptDocument receives ReceiptRenderer; OrderProgress stores OrderObserver subscriptions."],
        ["SRP","Creators construct products, wrappers add toppings, strategies calculate discounts, observers build audience messages and renderers format documents."],
        ["OCP","A new topping wrapper, discount or receipt renderer can implement the existing contract without changing the consumer's algorithm."],
        ["LSP","Decorated meals retain Meal identity/category and monetary contract; either renderer accepts the same ReceiptContent; tests exercise substitutions."],
        ["ISP","OrderObserver has one callback; ReceiptRenderer has one render operation; DiscountStrategy exposes only identity and discount calculation."],
        ["DIP","PricingCalculator depends on DiscountStrategy; ReceiptDocument on ReceiptRenderer; OrderProgress on OrderObserver, not audience classes."],
        ["KISS","An in-memory session book and synchronous observers meet the educational scope without queues, payment integration or a database."],
        ["DRY","Quotes and placement share construction/pricing; topping metadata is centralized; ReceiptDocument shares detail/item assembly across documents."],
    ],[120,WIDTH-120],small=True))
    add(Spacer(1,12),p("Tradeoffs explained","Sub"),p("The application service intentionally selects concrete strategies, observers and renderers at the composition boundary. The course's six-package constraint places HTTP support under builder rather than adding a seventh infrastructure package. These choices should be explained during defense, rather than presented as universal architectural rules.","Small"))
    page()

    # 13. Verification from actual local reports.
    add(*title("12 / Body - verification","Repeatable checks and observed results"))
    add(p(f"The current Maven Surefire evidence contains <b>{totals['tests']} tests, {totals['failures']} failures, {totals['errors']} errors and {totals['skipped']} skips</b>. Counts below are read from the XML reports produced by the real test run; they are not estimated from source annotations."))
    coverage={"MealFactoryTest":"Product types, definitions and factory validation", "OrderBuilderTest":"Configuration validation and immutable values", "PricingCalculatorTest":"Algorithms, eligibility, precision and invalid amounts", "ReceiptTest":"Both document types x both formats; HTML escaping", "ToppingDecoratorTest":"Composition, metadata, invalid choices and pricing", "OrderProgressTest":"Subscribers, state transitions and history", "RestaurantApiTest":"Quotes, placement, retries, sessions, receipts and errors", "PackageLayoutTest":"Exactly six package roots and only Main.java outside"}
    add(table(["Test suite","Tests","Evidence scope"],[[s['name'],str(s['tests']),coverage.get(s['name'],"Application integration and presentation boundary")] for s in suites],[158,38,WIDTH-196],small=True))
    add(Spacer(1,12),p("Reproduction commands (JDK 21)","Sub"))
    command_style=ParagraphStyle("Commands",fontName="Courier",fontSize=8,leading=12,textColor=INK)
    add(Preformatted(".\\mvnw.cmd verify\njava -jar target/hearth-and-table-1.0.0.jar --demo\njava -jar target/hearth-and-table-1.0.0.jar",command_style),Spacer(1,9))
    demo_ok=args.demo_log.exists() and "PASS" in args.demo_log.read_text(encoding="utf-8",errors="replace") and "all six patterns exercised successfully" in args.demo_log.read_text(encoding="utf-8",errors="replace")
    if not args.draft and not demo_ok:
        raise RuntimeError("Final export requires a saved successful six-pattern --demo output.")
    add(p("<b>Console demonstration:</b> "+("The saved run completed the built-in assertions and printed 'PASS - all six patterns exercised successfully.' It checks factory/builder totals, the 4700/4230 integration and eight observer events." if demo_ok else "Draft pending the final saved --demo verification log.")))
    add(p("<b>Browser evidence:</b> reviewed desktop and mobile captures on the following pages show the current English interface. The source documentation describes the interactive checks; final submission screenshots should be refreshed after the last UI change.","Small"))
    add(p("GitHub Actions is configured to run Maven verify and the console demo on pushes and pull requests. A local test report does not establish that the latest branch has passed remote CI or that it has been submitted to Moodle.","Small"))
    page()

    # 14. Desktop application screenshot.
    add(*title("13 / Body - application evidence","English restaurant menu"))
    add(p("The menu presents the restaurant identity, categories, prices and order entry. The English ITALIANO interface is a presentation layer over the shared Java API; the theme setting changes colors and logo contrast while preserving the ordering workflow.","Small"))
    add(photo(args.desktop_menu,WIDTH,595,args.draft,"Desktop menu - final English screenshot pending"),Spacer(1,9))
    add(p("Figure 8. English desktop menu in the dark theme after the final review. Food images are illustrative restaurant photography; source and license notes are retained in images/CREDITS.txt.","Caption"))
    page()

    # 15. Product customization + mobile order screenshots.
    add(*title("14 / Body - application evidence","Customization and mobile ordering"))
    add(photo(args.desktop_dialog,WIDTH,295,args.draft,"Desktop dish options - final screenshot pending"),Spacer(1,6),p("Figure 9. Light-theme dish options expose quantity and toppings; the displayed total is quoted by the server's shared construction/pricing path.","Caption"))
    mobile=photo(args.mobile_order,190,285,args.draft,"Mobile order pending")
    notes=p("<b>Figure 10. Mobile order.</b><br/><br/>The order view presents the current status, item prices and payable total. Customer/kitchen notifications originate from Java Observer publication. Receipt views use the Bridge document and format contracts.<br/><br/>Status advancement is explicitly a demonstration action. The system does not submit a real restaurant order or take payment.","Small")
    layout=Table([[mobile,notes]],colWidths=[205,WIDTH-205],hAlign="LEFT")
    layout.setStyle(TableStyle([("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),0),("RIGHTPADDING",(0,0),(-1,-1),12)]))
    add(layout)
    page()

    # 16. Explicit conclusion, further work and references.
    add(*title("15 / Conclusion and further work","What the project demonstrates"))
    add(p("ITALIANO integrates six distinct patterns into one restaurant ordering application. Anuar's Factory Method and Builder construct validated orders; Mukhammad's Decorator customizes meals; Akmaral's Strategy prices them; Mukhammad's Observer records audience notifications; and Akmaral's Bridge renders documents. Every student contributes two patterns through preserved implementation commits."))
    add(p(f"The {totals['tests']} automated tests and the assertion-based demo support functional correctness within the stated demonstration scope. The class and sequence diagrams show the implemented participants and interactions, while the screenshots connect those decisions to the user-facing application."))
    add(p("Further work","Sub"))
    bullets(["Persistence and access control: replace session-only storage with a database and authenticated customer/kitchen access, retaining pattern contracts.","Operational delivery: connect observers to a reliable asynchronous notification mechanism only when actual kitchen or messaging delivery is required.","Production ordering: add stock, cancellation/refund policies and a payment provider with explicit failure handling and auditing.","Quality and accessibility: extend browser automation, keyboard/screen-reader checks and theme contrast checks; retain the existing domain/API regression suite.","Catalog and deployment: move menu administration behind a validated interface and document a reproducible hosted environment with secure configuration."])
    add(p("Submission and oral defense","Sub"),p("The PDF and source ZIP are artifacts prepared for the course. Each student must upload the required files personally and be able to explain their two patterns, the Java roles, the integrated scenario, design tradeoffs and the team's Git history. Neither a generated report nor a successful local run proves that Moodle submission or the live defense has occurred.","Small"))
    add(p("References and evidence sources","Sub"),p("1. Course handout: Final Project Defense Requirements.pdf, five pages; assignment screenshot supplied with the task (grading, packaging and submission details).<br/>2. Project source and original commits: https://github.com/arynttai/sdp_final.<br/>3. Repository evidence: README.md, docs/AKMARAL.md, docs/MUKHAMMAD.md, src/main/java, src/test/java and target/surefire-reports.<br/>4. Editable report assets: docs/report/generate_report.py; docs/report/uml/*.puml and *.svg. Full Java sources are delivered in the source ZIP; selected exact excerpts are reproduced on pp. 5-10.<br/>5. Media provenance: src/main/resources/static/images/CREDITS.txt; local font license files accompany the application.","Small"))

    target=ROOT / ("tmp/pdfs/ITALIANO-report-draft.pdf" if args.draft else "output/pdf/ITALIANO-Final-Report.pdf")
    target.parent.mkdir(parents=True,exist_ok=True)
    document=SimpleDocTemplate(str(target),pagesize=A4,rightMargin=42,leftMargin=42,topMargin=53,bottomMargin=43,
        title="ITALIANO - Restaurant Ordering System - Final Project Report",author="Anuar, Akmaral, Mukhammad",subject="Six integrated software design patterns",pageCompression=1)
    document.build(story,canvasmaker=NumberedCanvas)
    count=len(PdfReader(target).pages)
    print(json.dumps({"output":str(target),"pages":count,"tests":totals,"demoVerified":demo_ok,"draft":args.draft},indent=2))
    if count!=16:
        raise RuntimeError(f"Expected 16 deliberately composed pages; got {count}. Inspect and fix layout.")
    (HERE / "evidence-manifest.json").write_text(json.dumps({"report":"output/pdf/ITALIANO-Final-Report.pdf","tests":totals,"suites":suites,"demoVerified":demo_ok,"sourceBaseline":"b5546ac","screenshots":{"desktopMenu":str(args.desktop_menu.relative_to(ROOT)) if args.desktop_menu.is_relative_to(ROOT) else str(args.desktop_menu),"desktopDialog":str(args.desktop_dialog.relative_to(ROOT)) if args.desktop_dialog.is_relative_to(ROOT) else str(args.desktop_dialog),"mobileOrder":str(args.mobile_order.relative_to(ROOT)) if args.mobile_order.is_relative_to(ROOT) else str(args.mobile_order)}},indent=2)+"\n",encoding="utf-8")


if __name__ == "__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--draft",action="store_true")
    parser.add_argument("--screenshots-ready",action="store_true")
    parser.add_argument("--desktop-menu",type=Path,default=HERE/"assets/dark-desktop-menu.png")
    parser.add_argument("--desktop-dialog",type=Path,default=HERE/"assets/light-desktop-dialog.png")
    parser.add_argument("--mobile-order",type=Path,default=HERE/"assets/dark-mobile-completed-order.png")
    parser.add_argument("--demo-log",type=Path,default=HERE/"demo-output.txt")
    options=parser.parse_args()
    if not options.draft and not options.screenshots_ready:
        parser.error("Final export requires --screenshots-ready after final English interface QA.")
    build(options)
