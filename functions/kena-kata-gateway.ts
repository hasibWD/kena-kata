import { createClient } from 'npm:@insforge/sdk';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
};

export default async function (req: Request): Promise<Response> {
  if (req.method === 'OPTIONS') {
    return new Response(null, { status: 204, headers: corsHeaders });
  }

  try {
    const body = req.method === 'POST' ? await req.json().catch(() => ({})) : {};
    const action = typeof body?.action === 'string' ? body.action : 'merchant_status';

    const bkashReady = Boolean(Deno.env.get('BKASH_APP_KEY') && Deno.env.get('BKASH_APP_SECRET'));
    const nagadReady = Boolean(Deno.env.get('NAGAD_MERCHANT_ID') && Deno.env.get('NAGAD_PRIVATE_KEY'));

    if (action === 'merchant_status') {
      return new Response(
        JSON.stringify({
          ok: true,
          vendorMode: 'SINGLE_VENDOR',
          manualPaymentModeActive: true,
          bkashMerchantApiConfigured: bkashReady,
          nagadMerchantApiConfigured: nagadReady,
          message:
            bkashReady || nagadReady
              ? 'Official Merchant Gateway credentials detected on server.'
              : 'Manual bKash/Nagad TrxID verification is active. Official Merchant API activates once merchant credentials are added to InsForge Secrets.',
        }),
        { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

    const client = createClient({
      baseUrl: Deno.env.get('INSFORGE_BASE_URL')!,
      anonKey: Deno.env.get('ANON_KEY')!,
    });

    const { data: products, error } = await client.database
      .from('products')
      .select('id,category_id,sold_count,rating_avg,is_trending,is_featured,stock_qty')
      .gt('stock_qty', 0);

    if (error) {
      return new Response(
        JSON.stringify({ ok: false, error: error.message }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

    const recentlyViewedIds: string[] = Array.isArray(body?.recentlyViewedIds) ? body.recentlyViewedIds : [];
    const list = Array.isArray(products) ? products : [];

    const viewedCats = new Set(
      list.filter((p: any) => recentlyViewedIds.includes(p.id)).map((p: any) => p.category_id)
    );

    const recommendedIds = [...list]
      .sort((a: any, b: any) => {
        const aScore = (viewedCats.has(a.category_id) ? 500 : 0) + Number(a.sold_count || 0) + Number(a.rating_avg || 0) * 20;
        const bScore = (viewedCats.has(b.category_id) ? 500 : 0) + Number(b.sold_count || 0) + Number(b.rating_avg || 0) * 20;
        return bScore - aScore;
      })
      .slice(0, 6)
      .map((p: any) => p.id);

    const trendingIds = [...list]
      .filter((p: any) => p.is_trending)
      .sort((a: any, b: any) => Number(b.sold_count || 0) - Number(a.sold_count || 0))
      .slice(0, 6)
      .map((p: any) => p.id);

    return new Response(
      JSON.stringify({
        ok: true,
        recommendedIds,
        trendingIds,
        source: 'insforge-edge-function',
      }),
      { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ ok: false, error: err instanceof Error ? err.message : String(err) }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    );
  }
}
