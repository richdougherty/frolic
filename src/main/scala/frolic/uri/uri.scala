package frolic.uri

import scala.collection.immutable

final case class PathSegment(value: String)
object PathSegment {
  def decode(encoded: String): PathSegment = PathSegment(encoded) // FIXME
}
final case class RelPath(segments: immutable.Seq[PathSegment])
final case class AbsPath(segments: immutable.Seq[PathSegment])
object AbsPath {
  def decode(encoded: String): AbsPath = {
    if (!encoded.startsWith("/")) throw new IllegalArgumentException("AbsPath must start with a slash")
    val deslashed = encoded.drop(1)
    if (deslashed.isEmpty) AbsPath(immutable.Seq.empty) else {
      val segments = StringUtils.splitString(deslashed, '/').map(PathSegment.decode)
      AbsPath(segments.to[immutable.Seq])
    }
  }
}
final case class Query(value: String /*form: Option[Form]*/)
object Query {
  def decode(encoded: String): Query = Query(encoded) // FIXME
}
//final case class Form(values: immutable.Seq[FormField])
//final case class FormField(value: String, encoded: String)
final case class AbsPathAndQuery(p: AbsPath, q: Option[Query])

object AbsPathAndQuery {
  def decode(encoded: String): AbsPathAndQuery = {
    val questionIndex = encoded.indexOf("?")
    val (encodedPath, query) = if (questionIndex == -1) {
      (encoded, None)
    } else {
      val encodedPath = encoded.substring(0, questionIndex) 
      val encodedQuery = encoded.substring(questionIndex+1) 
      (encodedPath, Some(Query.decode(encodedQuery)))
    }
    val absPath = AbsPath.decode(encodedPath)
    AbsPathAndQuery(absPath, query)
  }
}

private[uri] object StringUtils {
  /**
   * Split a string on a character. Similar to `String.split` except, for this method,
   * the invariant {{{splitString(s, '/').mkString("/") == s}}} holds.
   *
   * For example:
   * {{{
   * splitString("//a//", '/') == Seq("", "", "a", "", "")
   * String.split("//a//", '/') == Seq("", "", "a")
   * }}}
   */
  def splitString(s: String, c: Char): Seq[String] = {
    val result = scala.collection.mutable.ListBuffer.empty[String]
    import scala.annotation.tailrec
    @tailrec
    def splitLoop(start: Int): Unit = if (start < s.length) {
      var end = s.indexOf(c, start)
      if (end == -1) {
        result += s.substring(start)
      } else {
        result += s.substring(start, end)
        splitLoop(end + 1)
      }
    } else if (start == s.length) {
      result += ""
    }
    splitLoop(0)
    result
  }
}