package cortex.client.components.book

import cortex.client.components.book.widgets.{
  BTreeWalker,
  CacheStampedeSimulator,
  ConsistentHashRing,
  DecisionTree,
  DpTable,
  EstimationCalculator,
  HandshakeTimeline,
  HotShardSimulator,
  LatencyScaledTime,
  PartitionSimulator,
  QueueingSimulator,
  RaftAnimator,
  ReplicationLagSimulator
}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * Runtime dispatcher for `Block.D3Widget(name, payload)`. The widget catalog is a closed map from widget-name
 * → component; an unknown name renders an inline error so the rest of the chapter still mounts.
 *
 * New widgets land here as new cases. The structural decode in `shared.cortex.Blocks` is deliberately loose —
 * each widget owns the schema of its payload — so growing the catalog never touches shared.
 *
 * Phase C (Slice 10): names in [[KnownLayouts]] are routed to [[InlineDiagramBlock]] — the unified D3
 * `renderWidget` pipeline — before the bespoke match. Phase D (Slice 11) retired the four most-used bespoke
 * widgets (array-traversal, linked-list, binary-tree, graph-explorer) and their chapters.
 */
object D3WidgetBlock:

  /**
   * TS layout kinds that route to [[InlineDiagramBlock]] instead of the bespoke widget catalog.
   *
   * Slice 10: the five auto-dispatch layout kinds from Slice 3 (`array-1d`, `tree-binary`, `list-single`,
   * `list-double`, `hashmap`). Slice 11 adds `graph-generic` and retires the four bespoke widgets whose
   * chapters were migrated to this set.
   */
  private val KnownLayouts: Set[String] = Set(
    "array-1d",
    "tree-binary",
    "list-single",
    "list-double",
    "hashmap",
    "graph-generic",
    "trie",
    "stack",
    "queue",
    "union-find",
    "segment-tree",
    "fenwick",
    "bitset",
    "skiplist"
  )

  final case class Props(widget: String, payload: String)

  val Component = ScalaFnComponent[Props] { props =>
    // Phase C guard: layout kinds in KnownLayouts render via the unified InlineDiagramBlock
    // path (renderWidget in @d3/index). The legacy bespoke catalog handles everything else.
    if KnownLayouts.contains(props.widget) then
      InlineDiagramBlock.Component(InlineDiagramBlock.Props(props.widget, props.payload))
    else
      props.widget match
        case "latency-scaled-time" =>
          LatencyScaledTime.Component(LatencyScaledTime.Props(props.payload))
        case "decision-tree" =>
          DecisionTree.Component(DecisionTree.Props(props.payload))
        case "dp-table" =>
          DpTable.Component(DpTable.Props(props.payload))
        case "estimation-calculator" =>
          EstimationCalculator.Component(EstimationCalculator.Props(props.payload))
        case "partition-simulator" =>
          PartitionSimulator.Component(PartitionSimulator.Props(props.payload))
        case "queueing-simulator" =>
          QueueingSimulator.Component(QueueingSimulator.Props(props.payload))
        case "handshake-timeline" =>
          HandshakeTimeline.Component(HandshakeTimeline.Props(props.payload))
        case "consistent-hash-ring" =>
          ConsistentHashRing.Component(ConsistentHashRing.Props(props.payload))
        case "cache-stampede" =>
          CacheStampedeSimulator.Component(CacheStampedeSimulator.Props(props.payload))
        case "btree-walker" =>
          BTreeWalker.Component(BTreeWalker.Props(props.payload))
        case "replication-lag" =>
          ReplicationLagSimulator.Component(ReplicationLagSimulator.Props(props.payload))
        case "hot-shard" =>
          HotShardSimulator.Component(HotShardSimulator.Props(props.payload))
        case "raft-animator" =>
          RaftAnimator.Component(RaftAnimator.Props(props.payload))
        case other =>
          <.div(
            ^.className := "d3-widget__error",
            <.p(^.className := "d3-widget__error-title", "Unknown D3 widget"),
            <.p(
              ^.className := "d3-widget__error-message",
              s"""Widget "$other" is not registered. Available widgets: union-find, decision-tree, dp-table, latency-scaled-time, estimation-calculator, partition-simulator, queueing-simulator, handshake-timeline, consistent-hash-ring, cache-stampede, btree-walker, replication-lag, hot-shard, raft-animator."""
            )
          )
  }
