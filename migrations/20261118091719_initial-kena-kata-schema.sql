-- Kena Kata Single-Vendor E-Commerce Platform Schema & Security Migration
-- Strict Single-Vendor Rule: Only CUSTOMER and ADMIN roles exist (no SELLER role/commission/wallet).

CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;

-- 1. User Profiles (linked to auth.users)
CREATE TABLE public.profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  phone TEXT NOT NULL DEFAULT '',
  role TEXT NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'ADMIN')),
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  loyalty_points INTEGER NOT NULL DEFAULT 20 CHECK (loyalty_points >= 0),
  referral_code TEXT NOT NULL UNIQUE,
  referred_by_code TEXT NOT NULL DEFAULT '',
  avatar_url TEXT NOT NULL DEFAULT '',
  avatar_key TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER profiles_updated_at
  BEFORE UPDATE ON public.profiles
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- Helper: SECURITY DEFINER admin check (prevents RLS recursion)
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = auth.uid() AND role = 'ADMIN'
  );
$$;

-- Guard triggers on profiles so runtime users cannot escalate role or tamper with loyalty_points
CREATE OR REPLACE FUNCTION public.guard_profile_insert()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF current_user IN ('anon', 'authenticated') THEN
    NEW.role = 'CUSTOMER';
    NEW.loyalty_points = 20;
  END IF;
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_guard_profile_insert
  BEFORE INSERT ON public.profiles
  FOR EACH ROW EXECUTE FUNCTION public.guard_profile_insert();

CREATE OR REPLACE FUNCTION public.guard_profile_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF current_user IN ('anon', 'authenticated') AND NOT public.is_admin() THEN
    IF NEW.role IS DISTINCT FROM OLD.role THEN
      RAISE EXCEPTION 'role cannot be modified by customer';
    END IF;
    IF NEW.loyalty_points IS DISTINCT FROM OLD.loyalty_points THEN
      RAISE EXCEPTION 'loyalty_points are server-maintained';
    END IF;
    IF NEW.email IS DISTINCT FROM OLD.email THEN
      RAISE EXCEPTION 'email cannot be changed directly';
    END IF;
  END IF;
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_guard_profile_update
  BEFORE UPDATE ON public.profiles
  FOR EACH ROW EXECUTE FUNCTION public.guard_profile_update();

-- 2. Customer Addresses
CREATE TABLE public.addresses (
  id TEXT PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  label TEXT NOT NULL DEFAULT 'Home',
  full_name TEXT NOT NULL,
  phone TEXT NOT NULL,
  district TEXT NOT NULL,
  area TEXT NOT NULL DEFAULT '',
  street_address TEXT NOT NULL,
  is_default BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_addresses_user_id ON public.addresses(user_id);

-- 3. Categories
CREATE TABLE public.categories (
  id TEXT PRIMARY KEY,
  name_en TEXT NOT NULL,
  name_bn TEXT NOT NULL,
  slug TEXT NOT NULL UNIQUE,
  icon_key TEXT NOT NULL DEFAULT 'category',
  sort_order INTEGER NOT NULL DEFAULT 0,
  is_featured BOOLEAN NOT NULL DEFAULT TRUE
);

-- 4. Products (public storefront fields) & Product Costs (admin-only COGS)
CREATE TABLE public.products (
  id TEXT PRIMARY KEY,
  sku TEXT NOT NULL UNIQUE,
  barcode TEXT NOT NULL DEFAULT '',
  title_en TEXT NOT NULL,
  title_bn TEXT NOT NULL,
  brand TEXT NOT NULL DEFAULT 'Kena Kata',
  category_id TEXT NOT NULL REFERENCES public.categories(id) ON DELETE RESTRICT,
  description_en TEXT NOT NULL DEFAULT '',
  description_bn TEXT NOT NULL DEFAULT '',
  regular_price NUMERIC(12,2) NOT NULL CHECK (regular_price > 0),
  sale_price NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (sale_price >= 0),
  stock_qty INTEGER NOT NULL DEFAULT 0 CHECK (stock_qty >= 0),
  low_stock_threshold INTEGER NOT NULL DEFAULT 5 CHECK (low_stock_threshold >= 0),
  variants_csv TEXT NOT NULL DEFAULT 'Standard',
  colors_csv TEXT NOT NULL DEFAULT '',
  specs_pipe TEXT NOT NULL DEFAULT '',
  tags_csv TEXT NOT NULL DEFAULT '',
  is_featured BOOLEAN NOT NULL DEFAULT FALSE,
  is_trending BOOLEAN NOT NULL DEFAULT FALSE,
  is_best_seller BOOLEAN NOT NULL DEFAULT FALSE,
  is_new_arrival BOOLEAN NOT NULL DEFAULT FALSE,
  is_today_deal BOOLEAN NOT NULL DEFAULT FALSE,
  rating_avg NUMERIC(3,2) NOT NULL DEFAULT 4.60,
  review_count INTEGER NOT NULL DEFAULT 0 CHECK (review_count >= 0),
  sold_count INTEGER NOT NULL DEFAULT 0 CHECK (sold_count >= 0),
  seo_title TEXT NOT NULL DEFAULT '',
  seo_description TEXT NOT NULL DEFAULT '',
  image_res_key TEXT NOT NULL DEFAULT 'electronics',
  image_url TEXT NOT NULL DEFAULT '',
  image_key TEXT NOT NULL DEFAULT '',
  gallery_res_csv TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_products_category_id ON public.products(category_id);

CREATE TRIGGER products_updated_at
  BEFORE UPDATE ON public.products
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE public.product_costs (
  product_id TEXT PRIMARY KEY REFERENCES public.products(id) ON DELETE CASCADE,
  cost_price NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (cost_price >= 0),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER product_costs_updated_at
  BEFORE UPDATE ON public.product_costs
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- 5. Banners, Flash Sales, Coupons
CREATE TABLE public.banners (
  id TEXT PRIMARY KEY,
  title_en TEXT NOT NULL,
  title_bn TEXT NOT NULL,
  subtitle_en TEXT NOT NULL DEFAULT '',
  subtitle_bn TEXT NOT NULL DEFAULT '',
  badge_text TEXT NOT NULL DEFAULT '',
  target_category_id TEXT NOT NULL DEFAULT '',
  image_res_key TEXT NOT NULL DEFAULT 'hero_mega_sale',
  image_url TEXT NOT NULL DEFAULT '',
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE public.flash_sales (
  id TEXT PRIMARY KEY,
  title_en TEXT NOT NULL,
  title_bn TEXT NOT NULL,
  discount_percent INTEGER NOT NULL DEFAULT 10 CHECK (discount_percent BETWEEN 1 AND 95),
  start_time_millis BIGINT NOT NULL,
  end_time_millis BIGINT NOT NULL,
  product_ids_csv TEXT NOT NULL DEFAULT '',
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE public.coupons (
  id TEXT PRIMARY KEY,
  code TEXT NOT NULL UNIQUE,
  discount_type TEXT NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED')),
  discount_value NUMERIC(12,2) NOT NULL CHECK (discount_value > 0),
  min_order_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (min_order_amount >= 0),
  max_discount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (max_discount >= 0),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  expires_at_millis BIGINT NOT NULL,
  usage_count INTEGER NOT NULL DEFAULT 0 CHECK (usage_count >= 0)
);

-- 6. Cart, Wishlist, Comparison (for authenticated customers)
CREATE TABLE public.cart_items (
  id TEXT PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  product_id TEXT NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
  variant_label TEXT NOT NULL DEFAULT 'Standard',
  quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
  added_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_cart_user_id ON public.cart_items(user_id);
CREATE UNIQUE INDEX idx_cart_user_prod_var ON public.cart_items(user_id, product_id, variant_label);

CREATE TABLE public.wishlist_items (
  id TEXT PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  product_id TEXT NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
  added_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_wishlist_user_prod ON public.wishlist_items(user_id, product_id);

CREATE TABLE public.comparison_items (
  id TEXT PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  product_id TEXT NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
  added_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_compare_user_prod ON public.comparison_items(user_id, product_id);

-- 7. Orders, Order Items, Payment Records
CREATE TABLE public.orders (
  id TEXT PRIMARY KEY,
  order_number TEXT NOT NULL UNIQUE,
  idempotency_key TEXT NOT NULL UNIQUE,
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  guest_session_id TEXT NOT NULL DEFAULT '',
  customer_name TEXT NOT NULL,
  customer_phone TEXT NOT NULL,
  shipping_address TEXT NOT NULL,
  district TEXT NOT NULL,
  area TEXT NOT NULL DEFAULT '',
  subtotal NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
  delivery_charge NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (delivery_charge >= 0),
  delivery_cost_actual NUMERIC(12,2) NOT NULL DEFAULT 60 CHECK (delivery_cost_actual >= 0),
  discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
  loyalty_points_used INTEGER NOT NULL DEFAULT 0 CHECK (loyalty_points_used >= 0),
  loyalty_discount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (loyalty_discount >= 0),
  coupon_code TEXT NOT NULL DEFAULT '',
  total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
  payment_method TEXT NOT NULL CHECK (payment_method IN ('BKASH', 'NAGAD', 'COD')),
  payment_status TEXT NOT NULL CHECK (payment_status IN ('PENDING', 'SUBMITTED', 'VERIFIED', 'FAILED', 'REFUNDED')),
  order_status TEXT NOT NULL CHECK (order_status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'PACKED', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED')),
  courier_name TEXT NOT NULL DEFAULT 'Pending Assignment',
  tracking_number TEXT NOT NULL DEFAULT '',
  is_guest_order BOOLEAN NOT NULL DEFAULT FALSE,
  notes TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_orders_user_id ON public.orders(user_id);
CREATE INDEX idx_orders_guest_session ON public.orders(guest_session_id);

CREATE TRIGGER orders_updated_at
  BEFORE UPDATE ON public.orders
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE public.order_items (
  id TEXT PRIMARY KEY,
  order_id TEXT NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
  product_id TEXT NOT NULL,
  product_title_en TEXT NOT NULL,
  product_title_bn TEXT NOT NULL,
  variant_label TEXT NOT NULL DEFAULT 'Standard',
  unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
  cost_price NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (cost_price >= 0),
  quantity INTEGER NOT NULL CHECK (quantity > 0)
);
CREATE INDEX idx_order_items_order_id ON public.order_items(order_id);

CREATE TABLE public.payment_records (
  id TEXT PRIMARY KEY,
  order_id TEXT NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
  order_number TEXT NOT NULL,
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  guest_session_id TEXT NOT NULL DEFAULT '',
  method TEXT NOT NULL CHECK (method IN ('BKASH', 'NAGAD', 'COD')),
  amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
  sender_number TEXT NOT NULL DEFAULT '',
  transaction_id TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL CHECK (status IN ('PENDING', 'SUBMITTED', 'VERIFIED', 'FAILED', 'REFUNDED')),
  admin_note TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  verified_at TIMESTAMPTZ
);
CREATE INDEX idx_payments_order_id ON public.payment_records(order_id);
CREATE INDEX idx_payments_user_id ON public.payment_records(user_id);

-- 8. Reviews (trigger maintains product rating_avg and review_count)
CREATE TABLE public.reviews (
  id TEXT PRIMARY KEY,
  product_id TEXT NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  user_name TEXT NOT NULL,
  rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment TEXT NOT NULL,
  is_approved BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reviews_product_id ON public.reviews(product_id);

CREATE OR REPLACE FUNCTION public.refresh_product_rating()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
DECLARE
  v_pid TEXT;
  v_avg NUMERIC(3,2);
  v_cnt INTEGER;
BEGIN
  v_pid := COALESCE(NEW.product_id, OLD.product_id);
  SELECT COALESCE(ROUND(AVG(rating)::numeric, 2), 4.60), COUNT(*)::integer
    INTO v_avg, v_cnt
  FROM public.reviews
  WHERE product_id = v_pid AND is_approved = TRUE;

  UPDATE public.products
  SET rating_avg = v_avg, review_count = v_cnt
  WHERE id = v_pid;
  RETURN COALESCE(NEW, OLD);
END;
$$;

CREATE TRIGGER trg_reviews_refresh_rating
  AFTER INSERT OR UPDATE OR DELETE ON public.reviews
  FOR EACH ROW EXECUTE FUNCTION public.refresh_product_rating();

-- 9. Loyalty, Referrals, Notifications, Support, Activity Logs, Website Settings, Backups
CREATE TABLE public.loyalty_transactions (
  id TEXT PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  order_id TEXT NOT NULL DEFAULT '',
  unique_award_key TEXT NOT NULL UNIQUE,
  points_delta INTEGER NOT NULL,
  type TEXT NOT NULL CHECK (type IN ('EARNED', 'REDEEMED', 'REVERSED', 'REFERRAL')),
  description TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_loyalty_user_id ON public.loyalty_transactions(user_id);

CREATE TABLE public.referral_records (
  id TEXT PRIMARY KEY,
  referrer_user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  referred_user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  referred_user_email TEXT NOT NULL,
  referral_code TEXT NOT NULL,
  reward_points INTEGER NOT NULL DEFAULT 50,
  status TEXT NOT NULL DEFAULT 'COMPLETED',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_referral_referrer ON public.referral_records(referrer_user_id);

CREATE TABLE public.notifications (
  id TEXT PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  target_scope TEXT NOT NULL DEFAULT 'USER' CHECK (target_scope IN ('USER', 'ALL')),
  title_en TEXT NOT NULL,
  title_bn TEXT NOT NULL,
  message_en TEXT NOT NULL,
  message_bn TEXT NOT NULL,
  type TEXT NOT NULL DEFAULT 'INFO',
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_user_id ON public.notifications(user_id);

CREATE TABLE public.support_messages (
  id TEXT PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  sender_name TEXT NOT NULL,
  sender_email TEXT NOT NULL,
  subject TEXT NOT NULL,
  message TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'REPLIED')),
  admin_reply TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_support_user_id ON public.support_messages(user_id);

CREATE TABLE public.activity_logs (
  id TEXT PRIMARY KEY,
  actor_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  actor_email TEXT NOT NULL,
  action_type TEXT NOT NULL,
  target_type TEXT NOT NULL,
  target_id TEXT NOT NULL,
  summary TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE public.website_settings (
  id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),
  store_name TEXT NOT NULL DEFAULT 'Kena Kata',
  tagline_en TEXT NOT NULL DEFAULT 'Shop Smart, Buy Better.',
  tagline_bn TEXT NOT NULL DEFAULT 'বুদ্ধিমান কেনাকাটা, সেরা মানের নিশ্চয়তা।',
  announcement_en TEXT NOT NULL DEFAULT 'Free Dhaka Delivery on orders over ৳1,500 | Code: KENA10 for 10% OFF',
  announcement_bn TEXT NOT NULL DEFAULT '৳১,৫০০+ অর্ডারে ঢাকায় ফ্রি ডেলিভারি | ১০% ছাড়ে কোড: KENA10',
  bkash_merchant_number TEXT NOT NULL DEFAULT '01711-009988 (Merchant)',
  nagad_merchant_number TEXT NOT NULL DEFAULT '01811-009988 (Merchant)',
  whatsapp_number TEXT NOT NULL DEFAULT '8801711009988',
  support_email TEXT NOT NULL DEFAULT 'support@kenakata.bd',
  dhaka_delivery_fee NUMERIC(10,2) NOT NULL DEFAULT 60.00,
  outside_dhaka_delivery_fee NUMERIC(10,2) NOT NULL DEFAULT 120.00,
  loyalty_earn_per_100_bdt INTEGER NOT NULL DEFAULT 2,
  loyalty_point_value_bdt NUMERIC(10,2) NOT NULL DEFAULT 1.00,
  referral_reward_points INTEGER NOT NULL DEFAULT 50,
  newsletter_subscribers_count INTEGER NOT NULL DEFAULT 1420,
  show_flash_sale BOOLEAN NOT NULL DEFAULT TRUE,
  show_trending BOOLEAN NOT NULL DEFAULT TRUE,
  show_best_selling BOOLEAN NOT NULL DEFAULT TRUE,
  show_new_arrivals BOOLEAN NOT NULL DEFAULT TRUE,
  show_today_deals BOOLEAN NOT NULL DEFAULT TRUE,
  show_recommended BOOLEAN NOT NULL DEFAULT TRUE,
  show_reviews BOOLEAN NOT NULL DEFAULT TRUE,
  seo_home_title TEXT NOT NULL DEFAULT 'Kena Kata Bangladesh | Shop Smart, Buy Better',
  seo_home_description TEXT NOT NULL DEFAULT 'Single-vendor online store in Bangladesh for curated books, tech gadgets, fashion & lifestyle in BDT.',
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE public.backup_logs (
  id TEXT PRIMARY KEY,
  file_name TEXT NOT NULL,
  record_count INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL,
  actor_email TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============ ENABLE RLS & DEFINE GRANTS / POLICIES ============

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.addresses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product_costs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.banners ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.flash_sales ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.coupons ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cart_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.wishlist_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comparison_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.loyalty_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.referral_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.support_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.activity_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.website_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.backup_logs ENABLE ROW LEVEL SECURITY;

-- Restrict direct client writes on server-controlled tables
REVOKE INSERT, UPDATE, DELETE ON public.orders FROM anon, authenticated;
REVOKE INSERT, UPDATE, DELETE ON public.order_items FROM anon, authenticated;
REVOKE INSERT, UPDATE, DELETE ON public.payment_records FROM anon, authenticated;
REVOKE INSERT, UPDATE, DELETE ON public.loyalty_transactions FROM anon, authenticated;
REVOKE INSERT, UPDATE, DELETE ON public.referral_records FROM anon, authenticated;
REVOKE UPDATE, DELETE ON public.activity_logs FROM anon, authenticated;
REVOKE DELETE ON public.profiles FROM anon, authenticated;

-- Profiles
CREATE POLICY "profiles_select_own_or_admin" ON public.profiles
  FOR SELECT TO authenticated
  USING (id = auth.uid() OR public.is_admin());

CREATE POLICY "profiles_insert_self_customer" ON public.profiles
  FOR INSERT TO authenticated
  WITH CHECK (id = auth.uid() AND role = 'CUSTOMER');

CREATE POLICY "profiles_update_own_or_admin" ON public.profiles
  FOR UPDATE TO authenticated
  USING (id = auth.uid() OR public.is_admin())
  WITH CHECK (id = auth.uid() OR public.is_admin());

-- Addresses
CREATE POLICY "addresses_owner_or_admin_select" ON public.addresses
  FOR SELECT TO authenticated USING (user_id = auth.uid() OR public.is_admin());
CREATE POLICY "addresses_owner_insert" ON public.addresses
  FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());
CREATE POLICY "addresses_owner_update" ON public.addresses
  FOR UPDATE TO authenticated USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
CREATE POLICY "addresses_owner_delete" ON public.addresses
  FOR DELETE TO authenticated USING (user_id = auth.uid());

-- Categories
CREATE POLICY "categories_public_read" ON public.categories
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "categories_admin_insert" ON public.categories
  FOR INSERT TO authenticated WITH CHECK (public.is_admin());
CREATE POLICY "categories_admin_update" ON public.categories
  FOR UPDATE TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());
CREATE POLICY "categories_admin_delete" ON public.categories
  FOR DELETE TO authenticated USING (public.is_admin());

-- Products
CREATE POLICY "products_public_read" ON public.products
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "products_admin_insert" ON public.products
  FOR INSERT TO authenticated WITH CHECK (public.is_admin());
CREATE POLICY "products_admin_update" ON public.products
  FOR UPDATE TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());
CREATE POLICY "products_admin_delete" ON public.products
  FOR DELETE TO authenticated USING (public.is_admin());

-- Product Costs (admin-only wholesale margins)
CREATE POLICY "product_costs_admin_only" ON public.product_costs
  FOR ALL TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());

-- Banners
CREATE POLICY "banners_public_read" ON public.banners
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "banners_admin_write" ON public.banners
  FOR ALL TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());

-- Flash Sales
CREATE POLICY "flash_sales_public_read" ON public.flash_sales
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "flash_sales_admin_write" ON public.flash_sales
  FOR ALL TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());

-- Coupons
CREATE POLICY "coupons_read_active_or_admin" ON public.coupons
  FOR SELECT TO anon, authenticated USING (is_active = true OR public.is_admin());
CREATE POLICY "coupons_admin_write" ON public.coupons
  FOR ALL TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());

-- Cart, Wishlist, Compare
CREATE POLICY "cart_select_own_or_admin" ON public.cart_items
  FOR SELECT TO authenticated USING (user_id = auth.uid() OR public.is_admin());
CREATE POLICY "cart_insert_own" ON public.cart_items
  FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());
CREATE POLICY "cart_update_own" ON public.cart_items
  FOR UPDATE TO authenticated USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
CREATE POLICY "cart_delete_own" ON public.cart_items
  FOR DELETE TO authenticated USING (user_id = auth.uid());

CREATE POLICY "wishlist_own_all" ON public.wishlist_items
  FOR ALL TO authenticated USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());

CREATE POLICY "compare_own_all" ON public.comparison_items
  FOR ALL TO authenticated USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());

-- Orders, Order Items, Payment Records (reads for owner/admin; mutations via RPC)
CREATE POLICY "orders_select_own_or_admin" ON public.orders
  FOR SELECT TO authenticated USING (user_id = auth.uid() OR public.is_admin());

CREATE POLICY "order_items_select_own_or_admin" ON public.order_items
  FOR SELECT TO authenticated
  USING (
    public.is_admin() OR EXISTS (
      SELECT 1 FROM public.orders o WHERE o.id = order_id AND o.user_id = auth.uid()
    )
  );

CREATE POLICY "payments_select_own_or_admin" ON public.payment_records
  FOR SELECT TO authenticated USING (user_id = auth.uid() OR public.is_admin());

-- Reviews
CREATE POLICY "reviews_public_read" ON public.reviews
  FOR SELECT TO anon, authenticated USING (is_approved = true OR public.is_admin());
CREATE POLICY "reviews_insert_auth" ON public.reviews
  FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());
CREATE POLICY "reviews_insert_guest" ON public.reviews
  FOR INSERT TO anon WITH CHECK (user_id IS NULL);
CREATE POLICY "reviews_admin_delete" ON public.reviews
  FOR DELETE TO authenticated USING (public.is_admin());

-- Loyalty & Referrals
CREATE POLICY "loyalty_select_own_or_admin" ON public.loyalty_transactions
  FOR SELECT TO authenticated USING (user_id = auth.uid() OR public.is_admin());
CREATE POLICY "referrals_select_own_or_admin" ON public.referral_records
  FOR SELECT TO authenticated USING (referrer_user_id = auth.uid() OR referred_user_id = auth.uid() OR public.is_admin());

-- Notifications
CREATE POLICY "notifications_select_own_all_or_admin" ON public.notifications
  FOR SELECT TO anon, authenticated
  USING (target_scope = 'ALL' OR user_id = auth.uid() OR public.is_admin());
CREATE POLICY "notifications_update_own_read" ON public.notifications
  FOR UPDATE TO authenticated
  USING (user_id = auth.uid() OR public.is_admin())
  WITH CHECK (user_id = auth.uid() OR public.is_admin());
CREATE POLICY "notifications_admin_insert" ON public.notifications
  FOR INSERT TO authenticated WITH CHECK (public.is_admin());

-- Support Messages
CREATE POLICY "support_select_own_or_admin" ON public.support_messages
  FOR SELECT TO authenticated USING (user_id = auth.uid() OR public.is_admin());
CREATE POLICY "support_insert_auth" ON public.support_messages
  FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid() OR user_id IS NULL);
CREATE POLICY "support_insert_anon" ON public.support_messages
  FOR INSERT TO anon WITH CHECK (user_id IS NULL);
CREATE POLICY "support_admin_update" ON public.support_messages
  FOR UPDATE TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());

-- Activity Logs & Backup Logs (Admin only)
CREATE POLICY "activity_logs_admin_read" ON public.activity_logs
  FOR SELECT TO authenticated USING (public.is_admin());
CREATE POLICY "activity_logs_admin_insert" ON public.activity_logs
  FOR INSERT TO authenticated WITH CHECK (public.is_admin());

CREATE POLICY "backup_logs_admin_all" ON public.backup_logs
  FOR ALL TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());

-- Website Settings
CREATE POLICY "website_settings_public_read" ON public.website_settings
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "website_settings_admin_write" ON public.website_settings
  FOR ALL TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());

-- ============ SECURE SERVER-SIDE RPC FUNCTIONS ============

-- A. Ensure or Register Customer Profile (handles referral rewards server-side)
CREATE OR REPLACE FUNCTION public.ensure_customer_profile_rpc(
  p_name TEXT DEFAULT '',
  p_phone TEXT DEFAULT '',
  p_referral_code_input TEXT DEFAULT ''
)
RETURNS public.profiles
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
DECLARE
  v_uid UUID := auth.uid();
  v_auth_email TEXT;
  v_auth_verified BOOLEAN;
  v_existing public.profiles;
  v_ref_input TEXT := UPPER(TRIM(COALESCE(p_referral_code_input, '')));
  v_referrer public.profiles;
  v_settings public.website_settings;
  v_my_code TEXT;
  v_clean_name TEXT;
  v_initial_points INTEGER := 20;
  v_created public.profiles;
BEGIN
  IF v_uid IS NULL THEN
    RAISE EXCEPTION 'Authentication required';
  END IF;

  SELECT email, COALESCE(email_verified, false)
    INTO v_auth_email, v_auth_verified
  FROM auth.users
  WHERE id = v_uid;

  SELECT * INTO v_existing FROM public.profiles WHERE id = v_uid;
  IF FOUND THEN
    IF v_auth_verified AND NOT v_existing.email_verified THEN
      UPDATE public.profiles SET email_verified = true WHERE id = v_uid RETURNING * INTO v_existing;
    END IF;
    RETURN v_existing;
  END IF;

  v_clean_name := COALESCE(NULLIF(TRIM(p_name), ''), SPLIT_PART(v_auth_email, '@', 1));
  SELECT * INTO v_settings FROM public.website_settings WHERE id = 1;

  IF v_ref_input <> '' THEN
    SELECT * INTO v_referrer FROM public.profiles WHERE UPPER(referral_code) = v_ref_input;
    IF NOT FOUND THEN
      RAISE EXCEPTION 'Invalid referral code.';
    END IF;
    IF LOWER(v_referrer.email) = LOWER(v_auth_email) OR (TRIM(p_phone) <> '' AND v_referrer.phone = TRIM(p_phone)) THEN
      RAISE EXCEPTION 'Self-referral is not allowed.';
    END IF;
    v_initial_points := COALESCE(v_settings.referral_reward_points, 50);
  END IF;

  v_my_code := 'KK' || UPPER(SUBSTRING(REGEXP_REPLACE(v_clean_name, '[^A-Za-z0-9]', '', 'g') FROM 1 FOR 4)) || (1000 + FLOOR(RANDOM() * 9000)::INT)::TEXT;

  INSERT INTO public.profiles (
    id, email, name, phone, role, email_verified, loyalty_points, referral_code, referred_by_code
  ) VALUES (
    v_uid, LOWER(v_auth_email), v_clean_name, TRIM(COALESCE(p_phone, '')), 'CUSTOMER',
    v_auth_verified, v_initial_points, v_my_code, COALESCE(v_referrer.referral_code, '')
  )
  RETURNING * INTO v_created;

  IF v_referrer.id IS NOT NULL THEN
    INSERT INTO public.referral_records (
      id, referrer_user_id, referred_user_id, referred_user_email, referral_code, reward_points, status
    ) VALUES (
      'ref_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
      v_referrer.id, v_uid, LOWER(v_auth_email), v_referrer.referral_code,
      COALESCE(v_settings.referral_reward_points, 50), 'COMPLETED'
    );

    INSERT INTO public.loyalty_transactions (
      id, user_id, order_id, unique_award_key, points_delta, type, description
    ) VALUES (
      'loy_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
      v_referrer.id, '', 'REFERRAL_' || v_uid::TEXT,
      COALESCE(v_settings.referral_reward_points, 50), 'REFERRAL',
      'Referral reward for inviting ' || v_clean_name
    ) ON CONFLICT (unique_award_key) DO NOTHING;

    UPDATE public.profiles
    SET loyalty_points = loyalty_points + COALESCE(v_settings.referral_reward_points, 50)
    WHERE id = v_referrer.id;
  END IF;

  INSERT INTO public.notifications (
    id, user_id, target_scope, title_en, title_bn, message_en, message_bn, type
  ) VALUES (
    'notif_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
    v_uid, 'USER',
    'Welcome to Kena Kata!', 'কেনা কাটায় স্বাগতম!',
    'Explore curated books, gadgets, and heritage fashion in BDT.',
    'সেরা বই, গ্যাজেট ও দেশি ফ্যাশন সংগ্রহ দেখুন।',
    'ACCOUNT'
  );

  RETURN v_created;
END;
$$;

-- B. Atomic Order Placement RPC (Server-side price/stock/coupon/loyalty validation + idempotency)
CREATE OR REPLACE FUNCTION public.place_order_rpc(
  p_idempotency_key TEXT,
  p_guest_session_id TEXT,
  p_items JSONB,
  p_customer_name TEXT,
  p_customer_phone TEXT,
  p_district TEXT,
  p_area TEXT,
  p_street_address TEXT,
  p_coupon_code TEXT,
  p_redeem_loyalty BOOLEAN,
  p_payment_method TEXT,
  p_sender_number TEXT,
  p_transaction_id TEXT,
  p_notes TEXT
)
RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
DECLARE
  v_uid UUID := auth.uid();
  v_is_guest BOOLEAN := (auth.uid() IS NULL);
  v_existing public.orders;
  v_settings public.website_settings;
  v_order_id TEXT;
  v_order_number TEXT;
  v_subtotal NUMERIC(12,2) := 0;
  v_delivery_fee NUMERIC(12,2) := 0;
  v_delivery_actual NUMERIC(12,2) := 60;
  v_coupon public.coupons;
  v_coupon_discount NUMERIC(12,2) := 0;
  v_clean_coupon TEXT := UPPER(TRIM(COALESCE(p_coupon_code, '')));
  v_profile public.profiles;
  v_points_used INTEGER := 0;
  v_loyalty_discount NUMERIC(12,2) := 0;
  v_max_redeem_bdt NUMERIC(12,2) := 0;
  v_max_points INTEGER := 0;
  v_total NUMERIC(12,2) := 0;
  v_init_pay_status TEXT;
  v_item JSONB;
  v_prod public.products;
  v_cost NUMERIC(12,2);
  v_qty INTEGER;
  v_unit_price NUMERIC(12,2);
  v_variant TEXT;
  v_created_order public.orders;
BEGIN
  IF LENGTH(TRIM(COALESCE(p_customer_name, ''))) < 2 THEN
    RAISE EXCEPTION 'Customer name is required.';
  END IF;
  IF LENGTH(TRIM(COALESCE(p_customer_phone, ''))) < 10 THEN
    RAISE EXCEPTION 'Valid Bangladesh phone number is required.';
  END IF;
  IF LENGTH(TRIM(COALESCE(p_street_address, ''))) < 5 THEN
    RAISE EXCEPTION 'Full delivery address is required.';
  END IF;
  IF p_payment_method NOT IN ('BKASH', 'NAGAD', 'COD') THEN
    RAISE EXCEPTION 'Unsupported payment method.';
  END IF;
  IF p_payment_method IN ('BKASH', 'NAGAD') AND LENGTH(TRIM(COALESCE(p_transaction_id, ''))) < 6 THEN
    RAISE EXCEPTION 'Enter a valid % Transaction ID (TrxID) or select Cash on Delivery.', p_payment_method;
  END IF;
  IF p_items IS NULL OR JSONB_ARRAY_LENGTH(p_items) = 0 THEN
    RAISE EXCEPTION 'Your cart is empty.';
  END IF;

  SELECT * INTO v_existing FROM public.orders WHERE idempotency_key = p_idempotency_key;
  IF FOUND THEN
    RAISE EXCEPTION 'Duplicate submission prevented. Your order was already placed.';
  END IF;

  SELECT * INTO v_settings FROM public.website_settings WHERE id = 1;
  v_order_id := 'ord_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 10);
  v_order_number := 'KK-2026-' || (10100 + FLOOR(RANDOM() * 89000)::INT)::TEXT;

  -- First pass: lock products, verify stock, calculate server subtotal
  FOR v_item IN SELECT * FROM JSONB_ARRAY_ELEMENTS(p_items)
  LOOP
    v_qty := COALESCE((v_item->>'quantity')::INTEGER, 0);
    IF v_qty <= 0 THEN
      RAISE EXCEPTION 'Invalid item quantity.';
    END IF;
    SELECT * INTO v_prod FROM public.products WHERE id = (v_item->>'product_id') FOR UPDATE;
    IF NOT FOUND THEN
      RAISE EXCEPTION 'A product in your cart no longer exists.';
    END IF;
    IF v_prod.stock_qty < v_qty THEN
      RAISE EXCEPTION 'Insufficient stock for % (% left).', v_prod.title_en, v_prod.stock_qty;
    END IF;
    v_unit_price := CASE WHEN v_prod.sale_price > 0 AND v_prod.sale_price < v_prod.regular_price THEN v_prod.sale_price ELSE v_prod.regular_price END;
    v_subtotal := v_subtotal + (v_unit_price * v_qty);
  END LOOP;

  -- Delivery fee calculation (Bangladesh districts)
  IF LOWER(TRIM(p_district)) = 'dhaka' AND v_subtotal >= 1500 THEN
    v_delivery_fee := 0;
  ELSIF LOWER(TRIM(p_district)) = 'dhaka' THEN
    v_delivery_fee := COALESCE(v_settings.dhaka_delivery_fee, 60);
  ELSIF LOWER(TRIM(p_district)) IN ('gazipur', 'narayanganj', 'savar') THEN
    v_delivery_fee := COALESCE(v_settings.dhaka_delivery_fee, 60) + 30;
  ELSE
    v_delivery_fee := COALESCE(v_settings.outside_dhaka_delivery_fee, 120);
  END IF;
  v_delivery_actual := CASE WHEN LOWER(TRIM(p_district)) = 'dhaka' THEN 45 ELSE 95 END;

  -- Coupon validation
  IF v_clean_coupon <> '' THEN
    SELECT * INTO v_coupon FROM public.coupons WHERE code = v_clean_coupon FOR UPDATE;
    IF NOT FOUND THEN
      RAISE EXCEPTION 'Coupon % not found.', v_clean_coupon;
    END IF;
    IF NOT v_coupon.is_active THEN
      RAISE EXCEPTION 'This coupon is inactive.';
    END IF;
    IF v_coupon.expires_at_millis < (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT THEN
      RAISE EXCEPTION 'This coupon has expired.';
    END IF;
    IF v_subtotal < v_coupon.min_order_amount THEN
      RAISE EXCEPTION 'Minimum order ৳% required for %.', v_coupon.min_order_amount::INT, v_clean_coupon;
    END IF;
    IF v_coupon.discount_type = 'PERCENT' THEN
      v_coupon_discount := LEAST(v_subtotal * (v_coupon.discount_value / 100.0), v_coupon.max_discount);
    ELSE
      v_coupon_discount := LEAST(v_coupon.discount_value, v_subtotal);
    END IF;
    UPDATE public.coupons SET usage_count = usage_count + 1 WHERE code = v_clean_coupon;
  END IF;

  -- Loyalty redemption for authenticated user
  IF p_redeem_loyalty AND NOT v_is_guest THEN
    SELECT * INTO v_profile FROM public.profiles WHERE id = v_uid FOR UPDATE;
    IF FOUND AND v_profile.loyalty_points > 0 THEN
      v_max_redeem_bdt := GREATEST(0, (v_subtotal - v_coupon_discount) * 0.5);
      v_max_points := FLOOR(v_max_redeem_bdt / GREATEST(0.01, COALESCE(v_settings.loyalty_point_value_bdt, 1)))::INTEGER;
      v_points_used := LEAST(v_profile.loyalty_points, v_max_points);
      v_loyalty_discount := v_points_used * COALESCE(v_settings.loyalty_point_value_bdt, 1);
      IF v_points_used > 0 THEN
        UPDATE public.profiles SET loyalty_points = loyalty_points - v_points_used WHERE id = v_uid;
        INSERT INTO public.loyalty_transactions (
          id, user_id, order_id, unique_award_key, points_delta, type, description
        ) VALUES (
          'loy_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
          v_uid, v_order_id, 'REDEEM_' || v_order_id, -v_points_used, 'REDEEMED',
          'Redeemed on order ' || v_order_number
        );
      END IF;
    END IF;
  END IF;

  v_total := GREATEST(0, v_subtotal + v_delivery_fee - v_coupon_discount - v_loyalty_discount);
  -- CRITICAL PAYMENT RULE: Manual bKash/Nagad is SUBMITTED until Admin verifies; COD is PENDING
  v_init_pay_status := CASE WHEN p_payment_method = 'COD' THEN 'PENDING' ELSE 'SUBMITTED' END;

  INSERT INTO public.orders (
    id, order_number, idempotency_key, user_id, guest_session_id,
    customer_name, customer_phone, shipping_address, district, area,
    subtotal, delivery_charge, delivery_cost_actual, discount_amount,
    loyalty_points_used, loyalty_discount, coupon_code, total_amount,
    payment_method, payment_status, order_status, courier_name, tracking_number,
    is_guest_order, notes
  ) VALUES (
    v_order_id, v_order_number, p_idempotency_key, v_uid, COALESCE(p_guest_session_id, ''),
    TRIM(p_customer_name), TRIM(p_customer_phone), TRIM(p_street_address), TRIM(p_district), TRIM(COALESCE(p_area, '')),
    v_subtotal, v_delivery_fee, v_delivery_actual, v_coupon_discount,
    v_points_used, v_loyalty_discount, v_clean_coupon, v_total,
    p_payment_method, v_init_pay_status, 'PENDING', 'Pending Assignment', '',
    v_is_guest, TRIM(COALESCE(p_notes, ''))
  )
  RETURNING * INTO v_created_order;

  -- Second pass: insert order_items and deduct stock atomically
  FOR v_item IN SELECT * FROM JSONB_ARRAY_ELEMENTS(p_items)
  LOOP
    v_qty := (v_item->>'quantity')::INTEGER;
    v_variant := COALESCE(NULLIF(TRIM(v_item->>'variant_label'), ''), 'Standard');
    SELECT * INTO v_prod FROM public.products WHERE id = (v_item->>'product_id');
    SELECT COALESCE(cost_price, 0) INTO v_cost FROM public.product_costs WHERE product_id = v_prod.id;
    v_unit_price := CASE WHEN v_prod.sale_price > 0 AND v_prod.sale_price < v_prod.regular_price THEN v_prod.sale_price ELSE v_prod.regular_price END;

    INSERT INTO public.order_items (
      id, order_id, product_id, product_title_en, product_title_bn, variant_label, unit_price, cost_price, quantity
    ) VALUES (
      'oi_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
      v_order_id, v_prod.id, v_prod.title_en, v_prod.title_bn, v_variant, v_unit_price, COALESCE(v_cost, 0), v_qty
    );

    UPDATE public.products
    SET stock_qty = stock_qty - v_qty,
        sold_count = sold_count + v_qty
    WHERE id = v_prod.id AND stock_qty >= v_qty;
  END LOOP;

  INSERT INTO public.payment_records (
    id, order_id, order_number, user_id, guest_session_id, method, amount, sender_number, transaction_id, status, admin_note
  ) VALUES (
    'pay_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
    v_order_id, v_order_number, v_uid, COALESCE(p_guest_session_id, ''),
    p_payment_method, v_total, TRIM(COALESCE(p_sender_number, '')), UPPER(TRIM(COALESCE(p_transaction_id, ''))),
    v_init_pay_status,
    CASE WHEN p_payment_method = 'COD' THEN 'Collect cash on delivery' ELSE 'Submitted TrxID awaiting Admin verification' END
  );

  IF NOT v_is_guest THEN
    DELETE FROM public.cart_items WHERE user_id = v_uid;
    INSERT INTO public.notifications (
      id, user_id, target_scope, title_en, title_bn, message_en, message_bn, type
    ) VALUES (
      'notif_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
      v_uid, 'USER',
      'Order ' || v_order_number || ' Placed',
      'অর্ডার ' || v_order_number || ' গৃহীত হয়েছে',
      CASE WHEN p_payment_method = 'COD' THEN 'Cash on Delivery order received. We will confirm shortly.' ELSE p_payment_method || ' TrxID submitted. Owner verification pending.' END,
      CASE WHEN p_payment_method = 'COD' THEN 'ক্যাশ অন ডেলিভারি অর্ডার সফল হয়েছে।' ELSE p_payment_method || ' TrxID জমা হয়েছে। অ্যাডমিন যাচাইয়ের অপেক্ষায়।' END,
      'ORDER'
    );
  END IF;

  RETURN v_created_order;
END;
$$;

-- C. Guest Order Lookup & Order Items RPC
CREATE OR REPLACE FUNCTION public.get_guest_orders_rpc(p_guest_session_id TEXT)
RETURNS SETOF public.orders
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
  SELECT * FROM public.orders
  WHERE is_guest_order = TRUE AND guest_session_id = p_guest_session_id AND p_guest_session_id <> ''
  ORDER BY created_at DESC;
$$;

CREATE OR REPLACE FUNCTION public.get_order_items_rpc(p_order_id TEXT, p_guest_session_id TEXT DEFAULT '')
RETURNS SETOF public.order_items
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
  SELECT oi.*
  FROM public.order_items oi
  JOIN public.orders o ON o.id = oi.order_id
  WHERE oi.order_id = p_order_id
    AND (
      public.is_admin()
      OR (auth.uid() IS NOT NULL AND o.user_id = auth.uid())
      OR (o.is_guest_order = TRUE AND o.guest_session_id = p_guest_session_id AND p_guest_session_id <> '')
    );
$$;

-- D. Customer Cancel / Return Order RPC
CREATE OR REPLACE FUNCTION public.customer_cancel_or_return_order_rpc(
  p_order_id TEXT,
  p_is_return BOOLEAN,
  p_guest_session_id TEXT DEFAULT ''
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
DECLARE
  v_order public.orders;
  v_item RECORD;
  v_earn public.loyalty_transactions;
BEGIN
  SELECT * INTO v_order FROM public.orders WHERE id = p_order_id FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Order not found.';
  END IF;

  IF NOT (
    (auth.uid() IS NOT NULL AND v_order.user_id = auth.uid())
    OR (v_order.is_guest_order AND v_order.guest_session_id = p_guest_session_id AND p_guest_session_id <> '')
  ) THEN
    RAISE EXCEPTION 'Unauthorized order action.';
  END IF;

  IF NOT p_is_return AND v_order.order_status NOT IN ('PENDING', 'CONFIRMED') THEN
    RAISE EXCEPTION 'Orders already packed or shipped cannot be cancelled directly. Request a return after delivery.';
  END IF;
  IF p_is_return AND v_order.order_status <> 'DELIVERED' THEN
    RAISE EXCEPTION 'Only delivered orders can be requested for return.';
  END IF;

  UPDATE public.orders
  SET order_status = CASE WHEN p_is_return THEN 'RETURNED' ELSE 'CANCELLED' END
  WHERE id = p_order_id;

  FOR v_item IN SELECT product_id, quantity FROM public.order_items WHERE order_id = p_order_id
  LOOP
    UPDATE public.products SET stock_qty = stock_qty + v_item.quantity WHERE id = v_item.product_id;
  END LOOP;

  IF NOT v_order.is_guest_order AND v_order.user_id IS NOT NULL THEN
    SELECT * INTO v_earn FROM public.loyalty_transactions WHERE unique_award_key = 'EARN_' || v_order.id;
    IF FOUND THEN
      INSERT INTO public.loyalty_transactions (
        id, user_id, order_id, unique_award_key, points_delta, type, description
      ) VALUES (
        'loy_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
        v_order.user_id, v_order.id, 'REV_' || v_order.id, -v_earn.points_delta, 'REVERSED',
        'Reversed loyalty points for ' || v_order.order_number
      ) ON CONFLICT (unique_award_key) DO NOTHING;

      IF FOUND THEN
        UPDATE public.profiles SET loyalty_points = GREATEST(0, loyalty_points - v_earn.points_delta) WHERE id = v_order.user_id;
      END IF;
    END IF;
  END IF;
END;
$$;

-- E. Newsletter Subscribe RPC
CREATE OR REPLACE FUNCTION public.subscribe_newsletter_rpc()
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
DECLARE
  v_count INTEGER;
BEGIN
  UPDATE public.website_settings
  SET newsletter_subscribers_count = newsletter_subscribers_count + 1
  WHERE id = 1
  RETURNING newsletter_subscribers_count INTO v_count;
  RETURN COALESCE(v_count, 0);
END;
$$;

-- F. Admin Verify Payment RPC
CREATE OR REPLACE FUNCTION public.admin_verify_payment_rpc(
  p_payment_id TEXT,
  p_approve BOOLEAN,
  p_note TEXT DEFAULT ''
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
DECLARE
  v_admin public.profiles;
  v_pay public.payment_records;
  v_order public.orders;
  v_settings public.website_settings;
  v_new_pay_status TEXT;
  v_next_order_status TEXT;
  v_earned INTEGER;
BEGIN
  IF NOT public.is_admin() THEN
    RAISE EXCEPTION 'Admin authorization required.';
  END IF;
  SELECT * INTO v_admin FROM public.profiles WHERE id = auth.uid();
  SELECT * INTO v_pay FROM public.payment_records WHERE id = p_payment_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Payment record not found.'; END IF;
  SELECT * INTO v_order FROM public.orders WHERE id = v_pay.order_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Order not found.'; END IF;
  SELECT * INTO v_settings FROM public.website_settings WHERE id = 1;

  v_new_pay_status := CASE WHEN p_approve THEN 'VERIFIED' ELSE 'FAILED' END;
  v_next_order_status := CASE WHEN p_approve AND v_order.order_status = 'PENDING' THEN 'CONFIRMED' ELSE v_order.order_status END;

  UPDATE public.payment_records
  SET status = v_new_pay_status,
      admin_note = COALESCE(NULLIF(TRIM(p_note), ''), CASE WHEN p_approve THEN 'Verified by Owner Admin' ELSE 'Rejected: TrxID mismatch' END),
      verified_at = NOW()
  WHERE id = p_payment_id;

  UPDATE public.orders
  SET payment_status = v_new_pay_status,
      order_status = v_next_order_status
  WHERE id = v_order.id;

  IF p_approve AND NOT v_order.is_guest_order AND v_order.user_id IS NOT NULL THEN
    v_earned := FLOOR((v_order.subtotal / 100.0) * COALESCE(v_settings.loyalty_earn_per_100_bdt, 2))::INTEGER;
    IF v_earned > 0 THEN
      INSERT INTO public.loyalty_transactions (
        id, user_id, order_id, unique_award_key, points_delta, type, description
      ) VALUES (
        'loy_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
        v_order.user_id, v_order.id, 'EARN_' || v_order.id, v_earned, 'EARNED',
        'Earned on verified order ' || v_order.order_number
      ) ON CONFLICT (unique_award_key) DO NOTHING;
      IF FOUND THEN
        UPDATE public.profiles SET loyalty_points = loyalty_points + v_earned WHERE id = v_order.user_id;
      END IF;
    END IF;
  END IF;

  IF v_order.user_id IS NOT NULL THEN
    INSERT INTO public.notifications (id, user_id, target_scope, title_en, title_bn, message_en, message_bn, type)
    VALUES (
      'notif_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
      v_order.user_id, 'USER',
      'Payment ' || v_new_pay_status || ' (' || v_order.order_number || ')',
      'পেমেন্ট স্ট্যাটাস: ' || v_new_pay_status || ' (' || v_order.order_number || ')',
      CASE WHEN p_approve THEN 'Your ' || v_pay.method || ' payment of ৳' || v_order.total_amount::INT || ' is verified.' ELSE 'Payment verification failed: ' || COALESCE(p_note, '') END,
      CASE WHEN p_approve THEN 'আপনার ৳' || v_order.total_amount::INT || ' পেমেন্ট যাচাই সম্পন্ন হয়েছে।' ELSE 'পেমেন্ট যাচাই ব্যর্থ হয়েছে।' END,
      'PAYMENT'
    );
  END IF;

  INSERT INTO public.activity_logs (id, actor_id, actor_email, action_type, target_type, target_id, summary)
  VALUES (
    'log_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 10),
    v_admin.id, v_admin.email, 'PAYMENT_VERIFY', 'PAYMENT', v_pay.id,
    CASE WHEN p_approve THEN 'Verified ' ELSE 'Rejected ' END || v_pay.method || ' for ' || v_order.order_number
  );
END;
$$;

-- G. Admin Refund Payment RPC
CREATE OR REPLACE FUNCTION public.admin_refund_payment_rpc(p_payment_id TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
DECLARE
  v_admin public.profiles;
  v_pay public.payment_records;
  v_order public.orders;
  v_earn public.loyalty_transactions;
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admin authorization required.'; END IF;
  SELECT * INTO v_admin FROM public.profiles WHERE id = auth.uid();
  SELECT * INTO v_pay FROM public.payment_records WHERE id = p_payment_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Payment not found.'; END IF;
  SELECT * INTO v_order FROM public.orders WHERE id = v_pay.order_id FOR UPDATE;

  UPDATE public.payment_records SET status = 'REFUNDED', admin_note = 'Refunded by Owner Admin' WHERE id = p_payment_id;
  UPDATE public.orders SET payment_status = 'REFUNDED', order_status = 'RETURNED' WHERE id = v_order.id;

  IF NOT v_order.is_guest_order AND v_order.user_id IS NOT NULL THEN
    SELECT * INTO v_earn FROM public.loyalty_transactions WHERE unique_award_key = 'EARN_' || v_order.id;
    IF FOUND THEN
      INSERT INTO public.loyalty_transactions (
        id, user_id, order_id, unique_award_key, points_delta, type, description
      ) VALUES (
        'loy_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
        v_order.user_id, v_order.id, 'REV_' || v_order.id, -v_earn.points_delta, 'REVERSED',
        'Reversed loyalty points for ' || v_order.order_number
      ) ON CONFLICT (unique_award_key) DO NOTHING;
      IF FOUND THEN
        UPDATE public.profiles SET loyalty_points = GREATEST(0, loyalty_points - v_earn.points_delta) WHERE id = v_order.user_id;
      END IF;
    END IF;
  END IF;

  INSERT INTO public.activity_logs (id, actor_id, actor_email, action_type, target_type, target_id, summary)
  VALUES (
    'log_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 10),
    v_admin.id, v_admin.email, 'PAYMENT_REFUND', 'PAYMENT', v_pay.id,
    'Refunded order ' || v_order.order_number
  );
END;
$$;

-- H. Admin Update Order & Courier RPC
CREATE OR REPLACE FUNCTION public.admin_update_order_rpc(
  p_order_id TEXT,
  p_new_status TEXT,
  p_courier_name TEXT,
  p_tracking_number TEXT
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
DECLARE
  v_admin public.profiles;
  v_order public.orders;
  v_settings public.website_settings;
  v_pay_status TEXT;
  v_earned INTEGER;
  v_earn public.loyalty_transactions;
  v_item RECORD;
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admin authorization required.'; END IF;
  SELECT * INTO v_admin FROM public.profiles WHERE id = auth.uid();
  SELECT * INTO v_order FROM public.orders WHERE id = p_order_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Order not found.'; END IF;
  SELECT * INTO v_settings FROM public.website_settings WHERE id = 1;

  v_pay_status := CASE
    WHEN p_new_status = 'DELIVERED' AND v_order.payment_method = 'COD' AND v_order.payment_status = 'PENDING' THEN 'VERIFIED'
    ELSE v_order.payment_status
  END;

  UPDATE public.orders
  SET order_status = p_new_status,
      payment_status = v_pay_status,
      courier_name = COALESCE(NULLIF(TRIM(p_courier_name), ''), v_order.courier_name),
      tracking_number = TRIM(COALESCE(p_tracking_number, ''))
  WHERE id = p_order_id;

  IF p_new_status = 'DELIVERED' AND v_pay_status = 'VERIFIED' THEN
    UPDATE public.payment_records
    SET status = 'VERIFIED', verified_at = NOW(), admin_note = 'Collected & Delivered'
    WHERE order_id = p_order_id;

    IF NOT v_order.is_guest_order AND v_order.user_id IS NOT NULL THEN
      v_earned := FLOOR((v_order.subtotal / 100.0) * COALESCE(v_settings.loyalty_earn_per_100_bdt, 2))::INTEGER;
      IF v_earned > 0 THEN
        INSERT INTO public.loyalty_transactions (
          id, user_id, order_id, unique_award_key, points_delta, type, description
        ) VALUES (
          'loy_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
          v_order.user_id, v_order.id, 'EARN_' || v_order.id, v_earned, 'EARNED',
          'Earned on delivered order ' || v_order.order_number
        ) ON CONFLICT (unique_award_key) DO NOTHING;
        IF FOUND THEN
          UPDATE public.profiles SET loyalty_points = loyalty_points + v_earned WHERE id = v_order.user_id;
        END IF;
      END IF;
    END IF;
  END IF;

  IF p_new_status IN ('CANCELLED', 'RETURNED') THEN
    IF v_order.order_status NOT IN ('CANCELLED', 'RETURNED') THEN
      FOR v_item IN SELECT product_id, quantity FROM public.order_items WHERE order_id = p_order_id
      LOOP
        UPDATE public.products SET stock_qty = stock_qty + v_item.quantity WHERE id = v_item.product_id;
      END LOOP;
    END IF;
    IF NOT v_order.is_guest_order AND v_order.user_id IS NOT NULL THEN
      SELECT * INTO v_earn FROM public.loyalty_transactions WHERE unique_award_key = 'EARN_' || v_order.id;
      IF FOUND THEN
        INSERT INTO public.loyalty_transactions (
          id, user_id, order_id, unique_award_key, points_delta, type, description
        ) VALUES (
          'loy_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
          v_order.user_id, v_order.id, 'REV_' || v_order.id, -v_earn.points_delta, 'REVERSED',
          'Reversed loyalty points for ' || v_order.order_number
        ) ON CONFLICT (unique_award_key) DO NOTHING;
        IF FOUND THEN
          UPDATE public.profiles SET loyalty_points = GREATEST(0, loyalty_points - v_earn.points_delta) WHERE id = v_order.user_id;
        END IF;
      END IF;
    END IF;
  END IF;

  IF v_order.user_id IS NOT NULL THEN
    INSERT INTO public.notifications (id, user_id, target_scope, title_en, title_bn, message_en, message_bn, type)
    VALUES (
      'notif_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 8),
      v_order.user_id, 'USER',
      'Order ' || v_order.order_number || ': ' || p_new_status,
      'অর্ডার ' || v_order.order_number || ': ' || p_new_status,
      CASE WHEN TRIM(COALESCE(p_tracking_number, '')) <> '' THEN 'Courier: ' || p_courier_name || ' | Tracking: ' || p_tracking_number ELSE 'Order status updated to ' || p_new_status END,
      CASE WHEN TRIM(COALESCE(p_tracking_number, '')) <> '' THEN 'কুরিয়ার: ' || p_courier_name || ' | ট্র্যাকিং: ' || p_tracking_number ELSE 'অর্ডার স্ট্যাটাস আপডেট: ' || p_new_status END,
      'SHIPPING'
    );
  END IF;

  INSERT INTO public.activity_logs (id, actor_id, actor_email, action_type, target_type, target_id, summary)
  VALUES (
    'log_' || SUBSTRING(REPLACE(gen_random_uuid()::TEXT, '-', '') FROM 1 FOR 10),
    v_admin.id, v_admin.email, 'ORDER_UPDATE', 'ORDER', v_order.id,
    'Order ' || v_order.order_number || ' -> ' || p_new_status
  );
END;
$$;
