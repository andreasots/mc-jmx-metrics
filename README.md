# JMX Metrics
Fabric mod to add additional JMX metrics. Designed for use with the [JMX Exporter](https://github.com/prometheus/jmx_exporter).

## Metrics
Metrics are added to the existing `net.minecraft.server.Server` MBean.

 * `minTickTime`: Minimum tick time in the observation window (ms)
 * `maxTickTime`: Maximum tick time in the observation window (ms)
 * `players`: Number of connected players
 * `maxPlayers`: The maximum number of players that can play on the server at the same time
 * `entities`: Currently loaded entities by dimension and type
 * `blockEntities`: Currently ticking block entities by dimension and type
 * `chunks`: Number of loaded chunks by dimension
