package com.example.util

data class HtmlTemplateItem(
    val title: String,
    val description: String,
    val defaultSubject: String,
    val htmlContent: String
)

object HtmlTemplates {

    val templates = listOf(
        HtmlTemplateItem(
            title = "دعوة / ترحيب رسمي (Formal Invitation)",
            description = "قالب أنيق ترحيبي مع زر تفاعلي وشعار ملون",
            defaultSubject = "أهلاً بك يا {name} - دعوة خاصة وانضمام",
            htmlContent = """
                <!DOCTYPE html>
                <html lang="ar" dir="rtl">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>رسالة ترحيبية</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; direction: rtl; text-align: right; }
                        .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.05); }
                        .header { background: linear-gradient(135deg, #0f766e, #14b8a6); color: #ffffff; padding: 30px; text-align: center; }
                        .header h1 { margin: 0; font-size: 24px; font-weight: 700; }
                        .content { padding: 30px; color: #334155; line-height: 1.8; font-size: 16px; }
                        .greeting { font-size: 20px; color: #0f766e; font-weight: bold; margin-bottom: 15px; }
                        .button-box { text-align: center; margin: 30px 0; }
                        .btn { background-color: #0f766e; color: #ffffff !important; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: bold; display: inline-block; font-size: 16px; box-shadow: 0 2px 8px rgba(15,118,110,0.3); }
                        .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 13px; color: #64748b; border-top: 1px solid #e2e8f0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>منصة الخدمات الإلكترونية</h1>
                        </div>
                        <div class="content">
                            <div class="greeting">مرحباً {name}،</div>
                            <p>يسعدنا أن نرحب بك مجدداً معنا في هذا اليوم ({date}). تم تسجيل بريدك الإلكتروني <strong>{email}</strong> بنجاح في قائمتنا البريدية المتميزة.</p>
                            <p>نحن نعمل باستمرار على تقديم أفضل الخدمات والتحديثات الحصرية التي تهم أعمالك وتساعدك على النجاح والتطور.</p>
                            <div class="button-box">
                                <a href="https://example.com" class="btn">استكشف الخدمات الآن</a>
                            </div>
                            <p>إذا كان لديك أي استفسار، لا تتردد في التواصل معنا ردًا على هذه الرسالة.</p>
                        </div>
                        <div class="footer">
                            <p>تم إرسال هذه الرسالة إلى {email} - جميع الحقوق محفوظة © {date}</p>
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()
        ),
        HtmlTemplateItem(
            title = "عرض ترويجي وخاص (Special Offer)",
            description = "قالب تسويقي جذاب يحتوي على بطاقات أسعار وزر للشراء",
            defaultSubject = "عرض خاص حصري لك يا {name}! 🎁",
            htmlContent = """
                <!DOCTYPE html>
                <html lang="ar" dir="rtl">
                <head>
                    <meta charset="UTF-8">
                    <title>عرض خاص</title>
                    <style>
                        body { font-family: Cairo, Arial, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 20px; direction: rtl; text-align: right; }
                        .card { max-width: 580px; margin: 0 auto; background: #1e293b; border-radius: 16px; border: 1px solid #334155; padding: 30px; }
                        .badge { background: #f59e0b; color: #000; padding: 6px 14px; border-radius: 20px; font-weight: bold; font-size: 12px; display: inline-block; }
                        .title { font-size: 26px; color: #818cf8; margin-top: 15px; }
                        .price-box { background: #0f172a; padding: 20px; border-radius: 12px; text-align: center; margin: 25px 0; border: 1px stroke #4338ca; }
                        .price { font-size: 36px; color: #10b981; font-weight: bold; }
                        .btn { display: block; background: linear-gradient(90deg, #4338ca, #6366f1); color: white !important; text-align: center; padding: 15px; border-radius: 10px; font-weight: bold; text-decoration: none; font-size: 18px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <span class="badge">خصم 50% لفترة محدودة</span>
                        <div class="title">أهلاً {name}، خصم خاص لبريدك!</div>
                        <p>بصفتك مشتركاً مميزاً لدينا ({email})، يسعدنا تقديم خصم حصري يستمر حتى نهاية الأسبوع.</p>
                        <div class="price-box">
                            <div>السعر قبل الخصم: <span style="text-decoration: line-through; color: #94a3b8;">$199</span></div>
                            <div class="price">$99 فقط</div>
                        </div>
                        <a href="https://example.com/claim" class="btn">احصل على الخصم الآن</a>
                    </div>
                </body>
                </html>
            """.trimIndent()
        ),
        HtmlTemplateItem(
            title = "نشرة إخبارية بسيطة (Newsletter)",
            description = "قالب خفيف لنشر المقالات والأخبار والمستجدات",
            defaultSubject = "النشرة الإخبارية الأسبوعية - {date}",
            htmlContent = """
                <!DOCTYPE html>
                <html lang="ar" dir="rtl">
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: system-ui, sans-serif; background: #fafafa; padding: 20px; direction: rtl; text-align: right; color: #222; }
                        .box { max-width: 600px; margin: auto; background: white; border: 1px solid #ddd; padding: 25px; border-radius: 8px; }
                        h2 { color: #2563eb; }
                        .article { border-bottom: 1px solid #eee; padding-bottom: 15px; margin-bottom: 15px; }
                    </style>
                </head>
                <body>
                    <div class="box">
                        <h2>النشرة الأسبوعية 🎉</h2>
                        <p>أهلاً {name}، إليك أبرز المقالات والأخبار لليوم ({date}):</p>
                        <div class="article">
                            <h3>1. التطورات الجديدة في الذكاء الاصطناعي</h3>
                            <p>أحدث التقنيات وأدوات الإنتاجية المتاحة للأفراد والشركات اليوم.</p>
                        </div>
                        <div class="article">
                            <h3>2. نصائح لتحسين أداء البريد الإلكتروني</h3>
                            <p>كيف تضمن وصول رسائلك إلى صندوق الوارد وتفادي السبام بسهولة.</p>
                        </div>
                        <p style="font-size: 12px; color: #888;">تم الإرسال إلى: {email}</p>
                    </div>
                </body>
                </html>
            """.trimIndent()
        )
    )
}
