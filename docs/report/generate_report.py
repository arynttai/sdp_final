"""Create the plain English final report from the project's real source files."""
from pathlib import Path
import argparse
import html
import textwrap
from PIL import Image as PILImage
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Preformatted, Image, Table, TableStyle
from uml_helpers import diagrams, scaled

HERE = Path(__file__).resolve().parent
ROOT = next(p for p in HERE.parents if (p / 'pom.xml').exists())
JAVA = ROOT / 'src/main/java/com/restaurant'
WIDTH = A4[0] - 144

# Windows embeds genuine Times New Roman. Standard PDF Times is a portable fallback.
if Path('C:/Windows/Fonts/times.ttf').exists():
    for alias, file in [('Report','times.ttf'),('ReportBold','timesbd.ttf'),('ReportItalic','timesi.ttf')]:
        pdfmetrics.registerFont(TTFont(alias, 'C:/Windows/Fonts/' + file))
    BODY, BOLD, ITALIC = 'Report', 'ReportBold', 'ReportItalic'
else:
    BODY, BOLD, ITALIC = 'Times-Roman', 'Times-Bold', 'Times-Italic'
    from reportlab.pdfbase.pdfmetrics import Font
    for alias, face in [('Report', BODY), ('ReportBold', BOLD), ('ReportItalic', ITALIC)]:
        pdfmetrics.registerFont(Font(alias, face, 'WinAnsiEncoding'))
pdfmetrics.registerFontFamily(BODY, normal=BODY, bold=BOLD, italic=ITALIC, boldItalic=BOLD)

body = ParagraphStyle('Body',fontName=BODY,fontSize=12,leading=18,textColor=colors.black,spaceAfter=8)
heading = ParagraphStyle('Heading',fontName=BOLD,fontSize=14,leading=19,textColor=colors.black,spaceBefore=8,spaceAfter=10)
center = ParagraphStyle('Center',parent=body,alignment=TA_CENTER,spaceAfter=6)
caption = ParagraphStyle('Caption',fontName=BODY,fontSize=10,leading=13,textColor=colors.black,spaceAfter=10)
source_style = ParagraphStyle('Source',fontName='Courier',fontSize=8.3,leading=11,textColor=colors.black,spaceAfter=6)
cell = ParagraphStyle('Cell',parent=body,fontSize=11,leading=15,spaceAfter=0)


def p(text, style=body):
    return Paragraph(text,style)


def excerpt(relative, start, count):
    lines=(JAVA/relative).read_text(encoding='utf-8').splitlines()
    index=next(i for i,line in enumerate(lines) if start in line)
    selected=lines[index:index+count]
    indent=min(len(line)-len(line.lstrip()) for line in selected if line.strip())
    selected=[line[indent:] for line in selected]
    # Wrap only at spaces outside Java quoted strings; tokens and literals stay intact.
    formatted=[]
    for line in selected:
        while len(line)>88:
            quote=False; escaped=False; spaces=[]
            for i,ch in enumerate(line):
                if ch=='"' and not escaped: quote=not quote
                if ch==' ' and not quote and 5<i<88: spaces.append(i)
                escaped=(ch=='\\' and not escaped)
            if not spaces: break
            cut=spaces[-1]
            formatted.append(line[:cut]); line='    '+line[cut:].lstrip()
        formatted.append(line)
    return [Preformatted('\n'.join(formatted),source_style),p('Source: '+relative+', lines '+str(index+1)+'-'+str(index+count)+'.',caption)]


def photo(filename, max_height, max_width=WIDTH):
    path=HERE/'assets'/filename
    with PILImage.open(path) as im: w,h=im.size
    scale=min(max_width/w,max_height/h)
    return Image(str(path),w*scale,h*scale,hAlign='CENTER')


def footer(canvas,doc):
    canvas.setFont(BODY,10)
    canvas.setFillColor(colors.black)
    canvas.drawCentredString(A4[0]/2,36,str(doc.page))


def build(output):
    figures=diagrams()
    story=[]
    def add(*items): story.extend(items)
    def page(): add(PageBreak())

    # 1. Title, introduction and ordinary project description.
    add(p('<b>ITALIANO Restaurant Ordering System</b>',center),p('Final Project Report',center),
        p('Anuar, Akmaral and Mukhammad',center),Spacer(1,8),p('1. Introduction',heading))
    add(p('We developed ITALIANO, a restaurant ordering demo in Java 21 and Spring Boot. A user chooses a meal, adds toppings, selects dine-in or takeaway, and places an order. The application calculates discounts, records status changes and generates customer receipts and kitchen tickets. The website provides an English interface with light and dark themes.'))
    add(p('The project applies six design patterns in one application. Each team member implemented two patterns. Factory Method and Builder are creational patterns, Decorator and Bridge are structural patterns, and Strategy and Observer are behavioral patterns.'))
    rows=[['Student','Implementation','Original commits'],
        ['Anuar','Factory Method and Builder','cd79d48\n2792aba'],
        ['Akmaral','Strategy and Bridge','f05a62e\nb78f7d6'],
        ['Mukhammad','Decorator and Observer','8c41231\nf70398d']]
    rows=[[p(html.escape(value).replace('\n','<br/>'),cell) for value in row] for row in rows]
    team=Table(rows,colWidths=[78,WIDTH-180,102],hAlign='LEFT')
    team.setStyle(TableStyle([('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),0),('RIGHTPADDING',(0,0),(-1,-1),8),('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),7)]))
    add(team,Spacer(1,8))
    add(p('The commits retain their original authors. The source code and contribution history are available at <link href="https://github.com/arynttai/sdp_final" color="black">https://github.com/arynttai/sdp_final</link>.'))
    add(p('This is an educational demo. Orders are kept in the current HTTP session and are removed when the session expires or the application restarts. The system does not process payments or send messages to an external restaurant service.'))
    page()

    # 2-7. One readable subsection for each required design pattern.
    sections=[
        ('factory','2. Implementation\n2.1 Factory Method',
         ['Anuar implemented Factory Method to create the different meal types. Meal is the product interface. Pizza, Pasta and Drink inherit common immutable state from BaseMeal. MealFactory is the abstract creator, and PizzaFactory, PastaFactory and DrinkFactory implement its createMeal method.',
          'MenuCatalog selects a creator using the meal category. Common validation stays in MealFactory.create, while each concrete creator constructs its own product. The rest of the application receives a Meal and does not need to construct a particular product class. This separates product creation from order processing.'],
         'factory/creator/MealFactory.java','public final Meal create',6,
         'Figure 1. Factory Method classes and creation relationships.'),
        ('builder','2.2 Builder',
         ['Anuar implemented Builder for orders with different combinations of customer details, fulfillment mode, table number, preparation note and meal lines. Order.Builder provides fluent methods and produces an immutable Order through build(). Its constructor is private, and the product makes a defensive copy of the item list.',
          'The builder checks required customer and fulfillment details, valid table use, note length and the number of order lines. OrderLine validates meal quantities. The same construction path is used by the console example and the web application. Encapsulation keeps partially constructed orders outside the rest of the system.'],
         'Main.java','Order dineIn = new Order.Builder()',8,
         'Figure 2. Order.Builder constructs the immutable Order product.'),
        ('decorator','2.3 Decorator',
         ['Mukhammad implemented Decorator for optional toppings. ToppingDecorator implements Meal and stores another Meal. ExtraCheese, Mushrooms and Olives are concrete decorators. A decorator can wrap another decorator, so toppings can be combined without creating a class for every possible pizza or pasta combination.',
          'The wrapper delegates the meal ID and category, and adds the topping name, description and price. The original meal is unchanged. ToppingCatalog accepts up to three distinct known toppings and rejects toppings on drinks. Builder and Strategy continue to work with the Meal interface, which shows composition and polymorphism.'],
         'decorator/component/ToppingDecorator.java','public final String id()',7,
         'Figure 3. Decorators preserve the Meal interface while wrapping a Meal.'),
        ('bridge','2.4 Bridge',
         ['Akmaral implemented Bridge to separate the purpose of a document from its output format. ReceiptDocument is the abstraction. CustomerReceipt and KitchenTicket prepare different content. ReceiptRenderer is the implementation interface, with TextReceiptRenderer and HtmlReceiptRenderer as concrete implementations.',
          'Each document can use either renderer. Customer receipts include the subtotal, discount and payable total; kitchen tickets include preparation information without prices. ReceiptDocument delegates formatting instead of inheriting a separate class for every document and format combination. The HTML renderer escapes dynamic text.'],
         'bridge/abstraction/ReceiptDocument.java','public final String render',5,
         'Figure 4. Receipt purpose and rendering format vary independently.'),
        ('strategy','2.5 Strategy',
         ['Akmaral implemented Strategy for discount calculation. DiscountStrategy defines code() and discount(Order). NoDiscount returns no discount, StudentDiscount applies 10%, and PickupDiscount applies 20% only to takeaway orders. PricingCalculator uses the selected strategy and returns a Pricing record.',
          'Prices use BigDecimal with two decimal places and HALF_UP rounding. The calculator rejects negative discounts and discounts larger than the subtotal. It calculates the discount once and subtracts it from the subtotal. Order.total() retains its original pre-discount meaning; Pricing.total() is the amount payable. The calculator depends on the strategy interface rather than a particular discount class.'],
         'strategy/concrete/StudentDiscount.java','public BigDecimal discount(Order order)',4,
         'Figure 5. Discount strategies are interchangeable algorithms.'),
        ('observer','2.6 Observer',
         ['Mukhammad implemented Observer for order-status notifications. OrderProgress is the subject. OrderObserver is the observer interface, and CustomerObserver and KitchenObserver produce audience-specific messages from the same StatusChange event. The resulting OrderNotification records are stored in the order history.',
          'Both observers are subscribed before PLACED is announced. An order then progresses through PREPARING, READY and COMPLETED. Skipped or backward transitions are rejected, and requesting the current status does not publish another event. The subject knows the observer interface and leaves message wording to its subscribers. Mutable progress is kept separate from the immutable order.'],
         'observer/subject/OrderProgress.java','private void publish()',6,
         'Figure 6. The subject publishes status changes to subscribed observers.'),
    ]
    for key,title,paragraphs,relative,start,count,label in sections:
        for part in title.split('\n'): add(p(part,heading))
        for paragraph in paragraphs: add(p(paragraph))
        add(scaled(figures[key],width=WIDTH,height=225),Spacer(1,7),p(label,caption))
        add(*excerpt(relative,start,count))
        page()

    # 8. Integrated behavior and clean-code rationale in ordinary prose.
    add(p('2.7 Integration and code organization',heading))
    add(p('OrderService coordinates the six patterns. MenuCatalog creates the selected Meal through Factory Method. ToppingCatalog wraps it with the selected decorators. Order.Builder combines decorated meals and quantities into an Order. PricingCalculator applies the selected discount, and OrderProgress notifies the customer and kitchen observers. ReceiptDocument then renders the order using a selected receipt type and renderer.'))
    add(p('The production Java code is organized under six package roots: factory, builder, decorator, bridge, strategy and observer. Their role subfolders separate creators and products, abstractions and implementations, contracts and concrete classes. Main.java is the only Java file directly under com.restaurant. The web adapters, application service and session repository are placed under builder to retain this layout.'))
    add(p('The design follows SOLID in concrete ways. Single responsibility (SRP) separates creation, pricing, progress and document formatting. Open/closed (OCP) is supported by adding implementations through existing interfaces. Liskov substitution (LSP) is shown by Meal implementations and decorators that preserve identity and category. Interface segregation (ISP) keeps contracts small; dependency inversion (DIP) makes consumers depend on DiscountStrategy, OrderObserver and ReceiptRenderer.'))
    add(p('Encapsulation protects immutable Order data, while inheritance and composition connect the pattern roles. KISS keeps the demo limited to a session book and synchronous notifications. DRY appears in the shared construction and pricing path, centralized topping metadata, and ReceiptDocument\'s reusable detail assembly.'))
    add(p('For example, two Margheritas with ExtraCheese cost 2 x (2000 + 350) = 4700 KZT. The STUDENT strategy subtracts 470 KZT, leaving 4230 KZT. The customer receipt uses that same total. Four order statuses generate eight notifications: one customer and one kitchen notification for each status.'))
    add(p('The build passed 65 tests with no failures, errors or skipped tests. The tests cover the six patterns, package layout, pricing precision, receipt escaping, HTTP validation, session isolation and repeated order requests. The packaged --demo run also completed its assertions successfully.'))
    add(p('The deployed application is available at <link href="https://hearth-table.178.62.244.88.sslip.io/" color="black">https://hearth-table.178.62.244.88.sslip.io/</link>.'))
    page()

    # 9. Ordinary screenshots without frames or decorative layout.
    add(p('3. Application results',heading))
    add(p('The screenshots show the English interface in both themes. Users can choose meals, add toppings and review the server-calculated price before placing an order.'))
    add(photo('dark-desktop-preview.png',270),Spacer(1,5),p('Figure 7. Desktop interface in the dark theme.',caption))
    add(photo('light-desktop-dialog.png',250),Spacer(1,5),p('Figure 8. Meal customization in the light theme.',caption))
    page()

    # 10. Mobile result followed by the required concluding sections.
    add(p('3.1 Mobile view',heading))
    mobile=photo('dark-mobile-completed-order.png',315,135)
    explanation=p('The mobile layout supports the same ordering process. This completed order shows the 4700 KZT subtotal, 470 KZT student discount and 4230 KZT total. The order details contain the customer and kitchen notifications. Receipt links use the same Bridge implementation as the desktop interface.')
    layout=Table([[mobile,explanation]],colWidths=[145,WIDTH-145],hAlign='LEFT')
    layout.setStyle(TableStyle([('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),0),('RIGHTPADDING',(0,0),(-1,-1),10),('TOPPADDING',(0,0),(-1,-1),0),('BOTTOMPADDING',(0,0),(-1,-1),0)]))
    add(layout,Spacer(1,5),p('Figure 9. Completed order on a mobile screen.',caption))
    add(p('4. Conclusion',heading),p('ITALIANO combines six design patterns in one restaurant ordering application. Each student implemented two patterns, and the original contribution commits remain in the repository. The integrated example, automated tests and application screenshots show how the patterns work together. The project also demonstrates the use of interfaces, composition and immutable values to keep the code understandable.'))
    add(p('5. Further work',heading),p('Further development could add persistent storage, authenticated customer and kitchen access, stock management and real payment processing. Notifications could be connected to a reliable delivery service. More browser and accessibility tests would help maintain the ordering flow and both themes as the interface grows.'))

    output.parent.mkdir(parents=True,exist_ok=True)
    doc=SimpleDocTemplate(str(output),pagesize=A4,leftMargin=72,rightMargin=72,topMargin=60,bottomMargin=60,
        title='ITALIANO Restaurant Ordering System - Final Project Report',author='Anuar, Akmaral, Mukhammad',pageCompression=1)
    doc.build(story,onFirstPage=footer,onLaterPages=footer)
    reader=PdfReader(output)
    print('OUTPUT',output)
    print('PAGES',len(reader.pages))
    if not 10 <= len(reader.pages) <= 12: raise RuntimeError('Review layout: expected 10-12 pages')


if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--output',type=Path,default=ROOT/'output/pdf/ITALIANO-Report.pdf')
    build(parser.parse_args().output)
