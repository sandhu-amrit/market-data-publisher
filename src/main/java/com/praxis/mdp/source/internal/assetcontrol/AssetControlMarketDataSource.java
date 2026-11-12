package com.praxis.mdp.source.internal.assetcontrol;

/**
 * Placeholder for the Asset Control vendor Java API adapter.
 *
 * <p>Not a Spring bean and not a {@code MarketDataSource} yet. When the vendor client is available,
 * implement the SPI here: page instrument ids, keep the resume cursor in the step {@code
 * ExecutionContext}, and register the bean in place of {@code FileMarketDataSource} ({@code
 * mdp.source.type=asset-control}).
 */
public final class AssetControlMarketDataSource {

  public static final String STATUS = "NOT_IMPLEMENTED";

  private AssetControlMarketDataSource() {}
}
