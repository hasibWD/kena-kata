-- Fix advisor performance warnings (FK & RLS indexes + (SELECT auth.uid()) initplan)
-- and lock down function execute permissions + SET search_path = ''

CREATE INDEX IF NOT EXISTS idx_cart_items_product_id ON public.cart_items(product_id);
CREATE INDEX IF NOT EXISTS idx_wishlist_items_product_id ON public.wishlist_items(product_id);
CREATE INDEX IF NOT EXISTS idx_comparison_items_product_id ON public.comparison_items(product_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON public.order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_referral_records_referred_user_id ON public.referral_records(referred_user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_target_scope ON public.notifications(target_scope);

-- Lock down search_path & execute privileges on SECURITY DEFINER functions
ALTER FUNCTION public.is_admin() SET search_path = '';
ALTER FUNCTION public.refresh_product_rating() SET search_path = '';
ALTER FUNCTION public.ensure_customer_profile_rpc(text, text, text) SET search_path = '';
ALTER FUNCTION public.place_order_rpc(text, text, jsonb, text, text, text, text, text, text, boolean, text, text, text, text) SET search_path = '';
ALTER FUNCTION public.get_guest_orders_rpc(text) SET search_path = '';
ALTER FUNCTION public.get_order_items_rpc(text, text) SET search_path = '';
ALTER FUNCTION public.customer_cancel_or_return_order_rpc(text, boolean, text) SET search_path = '';
ALTER FUNCTION public.subscribe_newsletter_rpc() SET search_path = '';
ALTER FUNCTION public.admin_verify_payment_rpc(text, boolean, text) SET search_path = '';
ALTER FUNCTION public.admin_refund_payment_rpc(text) SET search_path = '';
ALTER FUNCTION public.admin_update_order_rpc(text, text, text, text) SET search_path = '';

REVOKE EXECUTE ON FUNCTION public.refresh_product_rating() FROM PUBLIC, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.ensure_customer_profile_rpc(text, text, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.ensure_customer_profile_rpc(text, text, text) TO authenticated;
REVOKE EXECUTE ON FUNCTION public.admin_verify_payment_rpc(text, boolean, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_verify_payment_rpc(text, boolean, text) TO authenticated;
REVOKE EXECUTE ON FUNCTION public.admin_refund_payment_rpc(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_refund_payment_rpc(text) TO authenticated;
REVOKE EXECUTE ON FUNCTION public.admin_update_order_rpc(text, text, text, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_update_order_rpc(text, text, text, text) TO authenticated;

-- Wrap auth.uid() in (SELECT auth.uid()) for single evaluation per query
DROP POLICY IF EXISTS "profiles_select_own_or_admin" ON public.profiles;
CREATE POLICY "profiles_select_own_or_admin" ON public.profiles
  FOR SELECT TO authenticated USING (id = (SELECT auth.uid()) OR public.is_admin());

DROP POLICY IF EXISTS "profiles_insert_self_customer" ON public.profiles;
CREATE POLICY "profiles_insert_self_customer" ON public.profiles
  FOR INSERT TO authenticated WITH CHECK (id = (SELECT auth.uid()) AND role = 'CUSTOMER');

DROP POLICY IF EXISTS "profiles_update_own_or_admin" ON public.profiles;
CREATE POLICY "profiles_update_own_or_admin" ON public.profiles
  FOR UPDATE TO authenticated
  USING (id = (SELECT auth.uid()) OR public.is_admin())
  WITH CHECK (id = (SELECT auth.uid()) OR public.is_admin());

DROP POLICY IF EXISTS "addresses_owner_or_admin_select" ON public.addresses;
CREATE POLICY "addresses_owner_or_admin_select" ON public.addresses
  FOR SELECT TO authenticated USING (user_id = (SELECT auth.uid()) OR public.is_admin());
DROP POLICY IF EXISTS "addresses_owner_insert" ON public.addresses;
CREATE POLICY "addresses_owner_insert" ON public.addresses
  FOR INSERT TO authenticated WITH CHECK (user_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS "addresses_owner_update" ON public.addresses;
CREATE POLICY "addresses_owner_update" ON public.addresses
  FOR UPDATE TO authenticated USING (user_id = (SELECT auth.uid())) WITH CHECK (user_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS "addresses_owner_delete" ON public.addresses;
CREATE POLICY "addresses_owner_delete" ON public.addresses
  FOR DELETE TO authenticated USING (user_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS "cart_select_own_or_admin" ON public.cart_items;
CREATE POLICY "cart_select_own_or_admin" ON public.cart_items
  FOR SELECT TO authenticated USING (user_id = (SELECT auth.uid()) OR public.is_admin());
DROP POLICY IF EXISTS "cart_insert_own" ON public.cart_items;
CREATE POLICY "cart_insert_own" ON public.cart_items
  FOR INSERT TO authenticated WITH CHECK (user_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS "cart_update_own" ON public.cart_items;
CREATE POLICY "cart_update_own" ON public.cart_items
  FOR UPDATE TO authenticated USING (user_id = (SELECT auth.uid())) WITH CHECK (user_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS "cart_delete_own" ON public.cart_items;
CREATE POLICY "cart_delete_own" ON public.cart_items
  FOR DELETE TO authenticated USING (user_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS "wishlist_own_all" ON public.wishlist_items;
CREATE POLICY "wishlist_own_all" ON public.wishlist_items
  FOR ALL TO authenticated USING (user_id = (SELECT auth.uid())) WITH CHECK (user_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS "compare_own_all" ON public.comparison_items;
CREATE POLICY "compare_own_all" ON public.comparison_items
  FOR ALL TO authenticated USING (user_id = (SELECT auth.uid())) WITH CHECK (user_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS "orders_select_own_or_admin" ON public.orders;
CREATE POLICY "orders_select_own_or_admin" ON public.orders
  FOR SELECT TO authenticated USING (user_id = (SELECT auth.uid()) OR public.is_admin());

DROP POLICY IF EXISTS "order_items_select_own_or_admin" ON public.order_items;
CREATE POLICY "order_items_select_own_or_admin" ON public.order_items
  FOR SELECT TO authenticated
  USING (
    public.is_admin() OR EXISTS (
      SELECT 1 FROM public.orders o WHERE o.id = order_id AND o.user_id = (SELECT auth.uid())
    )
  );

DROP POLICY IF EXISTS "payments_select_own_or_admin" ON public.payment_records;
CREATE POLICY "payments_select_own_or_admin" ON public.payment_records
  FOR SELECT TO authenticated USING (user_id = (SELECT auth.uid()) OR public.is_admin());

DROP POLICY IF EXISTS "reviews_insert_auth" ON public.reviews;
CREATE POLICY "reviews_insert_auth" ON public.reviews
  FOR INSERT TO authenticated WITH CHECK (user_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS "loyalty_select_own_or_admin" ON public.loyalty_transactions;
CREATE POLICY "loyalty_select_own_or_admin" ON public.loyalty_transactions
  FOR SELECT TO authenticated USING (user_id = (SELECT auth.uid()) OR public.is_admin());

DROP POLICY IF EXISTS "referrals_select_own_or_admin" ON public.referral_records;
CREATE POLICY "referrals_select_own_or_admin" ON public.referral_records
  FOR SELECT TO authenticated USING (referrer_user_id = (SELECT auth.uid()) OR referred_user_id = (SELECT auth.uid()) OR public.is_admin());

DROP POLICY IF EXISTS "notifications_select_own_all_or_admin" ON public.notifications;
CREATE POLICY "notifications_select_own_all_or_admin" ON public.notifications
  FOR SELECT TO anon, authenticated
  USING (target_scope = 'ALL' OR user_id = (SELECT auth.uid()) OR public.is_admin());

DROP POLICY IF EXISTS "notifications_update_own_read" ON public.notifications;
CREATE POLICY "notifications_update_own_read" ON public.notifications
  FOR UPDATE TO authenticated
  USING (user_id = (SELECT auth.uid()) OR public.is_admin())
  WITH CHECK (user_id = (SELECT auth.uid()) OR public.is_admin());

DROP POLICY IF EXISTS "support_select_own_or_admin" ON public.support_messages;
CREATE POLICY "support_select_own_or_admin" ON public.support_messages
  FOR SELECT TO authenticated USING (user_id = (SELECT auth.uid()) OR public.is_admin());

DROP POLICY IF EXISTS "support_insert_auth" ON public.support_messages;
CREATE POLICY "support_insert_auth" ON public.support_messages
  FOR INSERT TO authenticated WITH CHECK (user_id = (SELECT auth.uid()) OR user_id IS NULL);
