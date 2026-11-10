import com.knoopbyknoop.core.*
import java.io.File

// Reads each published geometry file with its graph and writes it again; prints whether the bytes
// are the same, and writes them to the output directory for comparing builds.
fun main(args: Array<String>) {
  val dir = File(args[0]); val out = File(args[1]).apply { mkdirs() }
  for (network in listOf("cycling", "hiking")) {
    val graphBytes = File(dir, "$network.graph").readBytes()
    val graph = GraphFile.read(graphBytes).graph
    val bytes = File(dir, "$network.geometry").readBytes()
    val again = NetworkGeometry.read(bytes, graph, sha256Hex(graphBytes)).toBytes(sha256Hex(graphBytes))
    File(out, "$network.geometry").writeBytes(again)
    println("$network: ${bytes.size} bytes read, ${again.size} written, same: ${bytes.contentEquals(again)}")
  }
}
