from reportlab.graphics.shapes import Drawing, Group, Line, Polygon, PolyLine, Rect, String
from reportlab.lib import colors
INK = MUTED = GOLD = LINE = colors.black
PALE = colors.white
WIDTH = 451.2756

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

def scaled(drawing, width=WIDTH, height=None):
    factor = min(width/drawing.width, height/drawing.height if height else 1)
    if factor == 1:
        return drawing
    holder = Drawing(drawing.width*factor,drawing.height*factor)
    group = Group(drawing)
    group.scale(factor,factor)
    holder.add(group)
    return holder
