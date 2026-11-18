-- Seed Kena Kata storefront catalog, settings, banners, flash sales, coupons, reviews, and store notifications

INSERT INTO public.website_settings (id) VALUES (1) ON CONFLICT (id) DO NOTHING;

INSERT INTO public.categories (id, name_en, name_bn, slug, icon_key, sort_order, is_featured) VALUES
  ('cat_books', 'Books & Literature', 'বই ও সাহিত্য', 'books', 'book', 1, TRUE),
  ('cat_electronics', 'Gadgets & Tech', 'গ্যাজেট ও টেক', 'electronics', 'devices', 2, TRUE),
  ('cat_fashion', 'Deshi Fashion', 'দেশি ফ্যাশন', 'fashion', 'checkroom', 3, TRUE),
  ('cat_home', 'Home & Living', 'ঘর ও লাইফস্টাইল', 'home', 'weekend', 4, TRUE),
  ('cat_stationery', 'Study & Stationery', 'পড়াশোনা ও স্টেশনারি', 'stationery', 'edit', 5, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.products (
  id, sku, barcode, title_en, title_bn, brand, category_id, description_en, description_bn,
  regular_price, sale_price, stock_qty, low_stock_threshold, variants_csv, colors_csv,
  specs_pipe, tags_csv, is_featured, is_trending, is_best_seller, is_new_arrival, is_today_deal,
  rating_avg, review_count, sold_count, seo_title, seo_description, image_res_key, gallery_res_csv
) VALUES
  ('prod_himu_book', 'KK-BK-101', '8901234500101', 'Himu Shomogro (Vol. 1) Hardcover', 'হিমু সমগ্র (প্রথম খণ্ড) হার্ডকভার', 'Ananya Prokashoni', 'cat_books',
   'Collector''s edition hardcover compilation of classic Himu novels with crisp cream paper and gold-foil spine.',
   'হুমায়ূন আহমেদের কালজয়ী হিমু সিরিজের প্রথম খণ্ডের প্রিমিয়াম হার্ডকভার সংস্করণ। উন্নত মানের ক্রিম কাগজ ও দৃষ্টিনন্দন বাঁধাই।',
   750, 560, 38, 6, 'Hardcover Collector''s,Paperback Standard', 'Gold Foil Navy',
   'Author: Humayun Ahmed;;Language: Bengali;;Pages: 520;;Binding: Hardcover;;Publisher: Ananya',
   'book,himu,bengali novel,rokomari style,bestseller',
   TRUE, TRUE, TRUE, FALSE, TRUE, 4.90, 1, 195,
   'Himu Shomogro Vol 1 Hardcover | Kena Kata', 'Buy original Himu Shomogro Vol 1 hardcover in Bangladesh with fast Dhaka delivery.',
   'hero_rokomari_reads', 'hero_rokomari_reads,fashion_craft'),

  ('prod_anc_earbuds', 'KK-EL-201', '8901234500201', 'QCY MeloBuds ANC Wireless Earbuds', 'কিউসিওয়াই মেলোবাডস এএনসি ওয়্যারলেস ইয়ারবাডস', 'QCY Bangladesh', 'cat_electronics',
   '40dB hybrid active noise cancellation, 6-mic ENC crystal call quality, Bluetooth 5.3, and 30-hour USB-C battery life.',
   '৪০ ডিবি অ্যাক্টিভ নয়েজ ক্যান্সেলেশন, ৬-মাইক ক্লিয়ার কল কোয়ালিটি এবং ৩০ ঘণ্টা ব্যাটারি ব্যাকআপ।',
   3450, 2690, 19, 5, 'Standard Edition,With Silicone Case', 'Midnight Blue,Pearl White',
   'ANC: 40dB Hybrid;;Bluetooth: v5.3;;Battery: 30 Hours;;Waterproof: IPX5;;Warranty: 6 Months Official',
   'earbuds,anc,audio,bluetooth,gadget',
   TRUE, TRUE, TRUE, TRUE, TRUE, 4.80, 1, 142,
   'QCY MeloBuds ANC Earbuds Price in BD | Kena Kata', 'Authentic QCY MeloBuds ANC earbuds in Bangladesh with warranty and bKash/COD checkout.',
   'electronics', 'electronics,hero_mega_sale'),

  ('prod_jamdani_panjabi', 'KK-FS-301', '8901234500301', 'Tangail Woven Cotton Panjabi', 'টাঙ্গাইল হ্যান্ডলুম কটন পাঞ্জাবি', 'Kena Kata Heritage', 'cat_fashion',
   'Breathable 100% combed Bangladeshi cotton panjabi tailored for Dhaka humidity with subtle jamdani collar embroidery.',
   'খাঁটি টাঙ্গাইল সুতির আরামদায়ক পাঞ্জাবি, সূক্ষ্ম জামদানি কারুকাজ ও আধুনিক ফিটিং।',
   2200, 1650, 24, 5, 'M (40),L (42),XL (44)', 'Royal Blue,Ivory Cream,Charcoal',
   'Fabric: 100% Tangail Cotton;;Fit: Regular Comfort;;Collar: Mandarin Jamdani;;Origin: Tangail, BD',
   'panjabi,cotton,eid,fashion,bangladesh',
   TRUE, TRUE, FALSE, TRUE, FALSE, 4.70, 1, 87,
   'Tangail Cotton Panjabi | Kena Kata Bangladesh', 'Handloom Tangail cotton panjabi with authentic jamdani detailing.',
   'fashion_craft', 'fashion_craft,hero_rokomari_reads'),

  ('prod_mech_keyboard', 'KK-EL-204', '8901234500204', 'Keychron K2 Hot-Swappable Mechanical Keyboard', 'কীক্রোন কে২ মেকানিক্যাল কীবোর্ড', 'Keychron', 'cat_electronics',
   'Compact 75% wireless/wired mechanical keyboard with Gateron Brown switches, Mac/Windows keycaps, and white backlight.',
   '৭৫% কমপ্যাক্ট ওয়্যারলেস ও ওয়্যার্ড মেকানিক্যাল কীবোর্ড, গ্যাটারন ব্রাউন সুইচসহ।',
   8900, 7490, 4, 5, 'Gateron Brown,Gateron Red', 'Slate Gray & Orange',
   'Layout: 75% (84 Keys);;Connectivity: Bluetooth 5.1 & USB-C;;Battery: 4000mAh;;Hot-Swappable: Yes',
   'keyboard,mechanical,developer,gaming,tech',
   TRUE, FALSE, TRUE, FALSE, TRUE, 4.90, 0, 63,
   'Keychron K2 Mechanical Keyboard BD | Kena Kata', 'Official Keychron K2 hot-swappable keyboard in Bangladesh.',
   'electronics', 'electronics,hero_mega_sale'),

  ('prod_powerbank_20k', 'KK-EL-208', '8901234500208', 'Anker PowerCore 20,000mAh 22.5W Fast Power Bank', 'অ্যাঙ্কার ২০,০০০ এমএএইচ ফাস্ট পাওয়ার ব্যাংক', 'Anker', 'cat_electronics',
   'High-capacity 20,000mAh portable charger with USB-C PD 22.5W fast charging and dual USB-A ports.',
   '২২.৫ ওয়াট ফাস্ট চার্জিং সুবিধাসহ ২০,০০০ এমএএইচ পাওয়ার ব্যাংক।',
   3200, 2750, 16, 5, '20,000mAh 22.5W', 'Matte Black,Navy Blue',
   'Capacity: 20,000mAh;;Fast Charge: 22.5W PD;;Ports: 1x USB-C, 2x USB-A;;Warranty: 12 Months',
   'powerbank,anker,charger,travel',
   FALSE, TRUE, TRUE, FALSE, FALSE, 4.80, 0, 210,
   '', '', 'hero_mega_sale', 'hero_mega_sale,electronics'),

  ('prod_atomic_habits_bn', 'KK-BK-105', '8901234500105', 'Atomic Habits (Bengali Authorized Edition)', 'অ্যাটমিক হ্যাবিটস (বাংলা অনুবাদ সংস্করণ)', 'Adarsha Prokashoni', 'cat_books',
   'James Clear''s transformative guide to building good habits and breaking bad ones, cleanly translated into natural Bengali.',
   'জেমস ক্লিয়ারের বিশ্বখ্যাত বেস্টসেলার বইয়ের প্রাঞ্জল বাংলা অনুবাদ সংস্করণ।',
   480, 340, 52, 8, 'Paperback,Hardcover', 'Classic White',
   'Author: James Clear;;Pages: 312;;Language: Bengali;;Binding: Quality Paperback',
   'books,self help,habits,bengali,study',
   TRUE, TRUE, TRUE, TRUE, TRUE, 4.90, 0, 320,
   '', '', 'hero_rokomari_reads', 'hero_rokomari_reads'),

  ('prod_study_lamp', 'KK-HM-402', '8901234500402', 'Nordic Brass Eye-Care Study Desk Lamp', 'আই-কেয়ার পিতলের স্টাডি ডেস্ক ল্যাম্প', 'Alo Craft', 'cat_home',
   'Flicker-free 3-tone dimmable LED reading lamp with adjustable brass arm for late-night study and reading.',
   'চোখের আরামদায়ক ৩-টোন লাইটিং ও এডজাস্টেবল স্ট্যান্ডসহ প্রিমিয়াম স্টাডি ল্যাম্প।',
   1850, 1390, 11, 4, 'USB Rechargeable,AC Plug-in', 'Brushed Brass,Matte Black',
   'Color Temp: 3000K-6000K;;Power: 10W Eye-Care LED;;Material: Aluminum & Brass',
   'lamp,study,desk,home,decor',
   FALSE, FALSE, FALSE, TRUE, FALSE, 4.60, 0, 41,
   '', '', 'hero_rokomari_reads', 'hero_rokomari_reads'),

  ('prod_leather_wallet', 'KK-FS-309', '8901234500309', 'Full-Grain Bangladeshi Leather RFID Wallet', 'খাঁটি চামড়ার আরএফআইডি প্রোটেক্টেড ওয়ালেট', 'Tannery Craft BD', 'cat_fashion',
   'Hand-stitched full-grain cowhide bifold wallet crafted in Dhaka with RFID shielding and 8 card slots.',
   'ঢাকায় তৈরি খাঁটি গরুর চামড়ার হ্যান্ড-স্টিচড ওয়ালেট, ৮টি কার্ড স্লট ও আরএফআইডি সুরক্ষাসহ।',
   1250, 890, 0, 5, 'Standard Bifold,Gift Box Edition', 'Espresso Brown,Jet Black',
   'Material: 100% Full-Grain Leather;;RFID Shield: Yes;;Slots: 8 Cards + 2 Cash',
   'wallet,leather,gift,fashion',
   FALSE, FALSE, TRUE, FALSE, FALSE, 4.70, 0, 115,
   '', '', 'fashion_craft', 'fashion_craft'),

  ('prod_journal_kit', 'KK-ST-501', '8901234500501', 'Kena Kata Dotted Bullet Journal & Fountain Pen Kit', 'কেনা কাটা ডটেড জার্নাল ও ফাউন্টেন পেন সেট', 'PaperTrail BD', 'cat_stationery',
   '120gsm fountain-pen friendly dotted A5 notebook paired with a smooth brass-nib fountain pen and 2 ink cartridges.',
   '১২০ জিএসএম প্রিমিয়াম কাগজের ডটেড নোটবুক ও মসৃণ ফাউন্টেন পেন কম্বো সেট।',
   950, 720, 27, 5, 'A5 Dotted Kit,A5 Ruled Kit', 'Deep Blue Cover,Burnt Orange Cover',
   'Paper: 120gsm Acid-Free;;Pages: 192;;Pen Nib: Fine 0.5mm',
   'notebook,journal,pen,stationery,study',
   TRUE, TRUE, FALSE, TRUE, TRUE, 4.80, 0, 74,
   '', '', 'hero_rokomari_reads', 'hero_rokomari_reads')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.product_costs (product_id, cost_price) VALUES
  ('prod_himu_book', 410),
  ('prod_anc_earbuds', 2050),
  ('prod_jamdani_panjabi', 1100),
  ('prod_mech_keyboard', 6100),
  ('prod_powerbank_20k', 2150),
  ('prod_atomic_habits_bn', 220),
  ('prod_study_lamp', 920),
  ('prod_leather_wallet', 540),
  ('prod_journal_kit', 440)
ON CONFLICT (product_id) DO NOTHING;

INSERT INTO public.banners (id, title_en, title_bn, subtitle_en, subtitle_bn, badge_text, target_category_id, image_res_key, is_active, sort_order) VALUES
  ('ban_1', 'Winter Mega Tech & Book Fair', 'উইন্টার মেগা টেক ও বই মেলা', 'Up to 30% OFF + 10% Extra with code KENA10', '৩০% পর্যন্ত ছাড় + KENA10 কোডে আরও ১০% সাশ্রয়', 'HOT DEAL', 'cat_electronics', 'hero_mega_sale', TRUE, 1),
  ('ban_2', 'Curated Bengali Classics & Study Kits', 'সেরা বাংলা বই ও স্টাডি কালেকশন', 'Authentic editions delivered across 64 districts', 'সারা দেশের ৬৪ জেলায় দ্রুত হোম ডেলিভারি', 'ROKOMARI PICK', 'cat_books', 'hero_rokomari_reads', TRUE, 2)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.flash_sales (id, title_en, title_bn, discount_percent, start_time_millis, end_time_millis, product_ids_csv, is_active) VALUES
  ('fs_winter_1', 'Midnight Lightning Deals', 'মিডনাইট ফ্ল্যাশ সেল', 25,
   (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT - 3600000,
   (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT + 72 * 3600000,
   'prod_himu_book,prod_anc_earbuds,prod_mech_keyboard,prod_atomic_habits_bn,prod_journal_kit',
   TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.coupons (id, code, discount_type, discount_value, min_order_amount, max_discount, is_active, expires_at_millis, usage_count) VALUES
  ('cpn_1', 'KENA10', 'PERCENT', 10, 500, 400, TRUE, (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT + 90::BIGINT * 86400000, 14),
  ('cpn_2', 'DHAKA50', 'FIXED', 50, 600, 50, TRUE, (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT + 90::BIGINT * 86400000, 8),
  ('cpn_3', 'BOOK20', 'PERCENT', 20, 400, 250, TRUE, (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT + 90::BIGINT * 86400000, 21)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.reviews (id, product_id, user_id, user_name, rating, comment, is_approved) VALUES
  ('rev_1', 'prod_himu_book', NULL, 'Hasibul Hasan', 5, 'Print quality and packaging were spotless. Arrived in Dhanmondi within 24 hours!', TRUE),
  ('rev_2', 'prod_anc_earbuds', NULL, 'Sadia Afrin', 5, 'ANC blocks Dhaka traffic noise really well. Verified bKash payment smoothly.', TRUE),
  ('rev_3', 'prod_jamdani_panjabi', NULL, 'Rafiqul Islam', 5, 'Soft Tangail cotton, accurate size chart, and great value in BDT.', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notifications (id, user_id, target_scope, title_en, title_bn, message_en, message_bn, type) VALUES
  ('notif_all_1', NULL, 'ALL', 'Midnight Flash Sale Live!', 'মিডনাইট ফ্ল্যাশ সেল শুরু হয়েছে!',
   'Save up to 25% on books and ANC gadgets. Use KENA10 at checkout.',
   'বই ও গ্যাজেটে ২৫% পর্যন্ত ছাড়! চেকআউটে KENA10 কোড ব্যবহার করুন।', 'FLASH_SALE')
ON CONFLICT (id) DO NOTHING;
