package codefolio.server.http

import zio.*
import zio.http.*

import java.io.File
import java.nio.file.Path as NioPath

/**
 * Serves files from a fixed root directory with path-traversal containment.
 *
 * Two callers in production — [[StaticRoutes]] (the Vite frontend bundle) and [[CortexAssetRoutes]]
 * (chapter-relative binary assets). Each builds its own `FileServer` over its own root; this module owns the
 * security-critical part once: resolve a candidate to its real on-disk path, reject anything that escapes the
 * root (`..` segments, absolute-path inputs, symlinks pointing outside), read the bytes, attach a
 * Content-Type from [[ContentTypes]], and 404 on a miss, a directory, or a non-existent root.
 *
 * `exists` is false in dev mode (no Vite `dist/` on disk yet) — every [[serve]] then 404s, and the caller
 * decides whether to mount routes at all. Resolving the root once (at construction) keeps the per-request
 * path cheap.
 */
final class FileServer private (
    val root: File,
    val exists: Boolean,
    rootRealPath: NioPath
):

  /**
   * Resolve `rel` under the root and serve the file, or 404. `rel` is treated as relative to the root
   * regardless of a leading slash. Rejects any candidate whose real path escapes the root — the one
   * load-bearing security check on these routes.
   */
  def serve(rel: String): ZIO[Any, Nothing, Response] =
    val cleaned = rel.stripPrefix("/")
    if !exists || cleaned.isEmpty then ZIO.succeed(Response.status(Status.NotFound))
    else
      val candidate = File(root, cleaned)
      // toRealPath resolves symlinks + `..`; it throws on a missing file, in which case there is nothing
      // to serve anyway — fall through to 404.
      val candidateReal =
        try Some(candidate.toPath.toRealPath())
        catch case _: Throwable => None
      candidateReal match
        case Some(real) if real.startsWith(rootRealPath) && candidate.isFile =>
          readAndRespond(candidate)
        case _ => ZIO.succeed(Response.status(Status.NotFound))

  private def readAndRespond(file: File): ZIO[Any, Nothing, Response] =
    ZIO
      .attemptBlockingIO(file.exists() && file.isFile())
      .flatMap {
        case true =>
          Body.fromFile(file).map { b =>
            Response(
              status = Status.Ok,
              headers = Headers(
                Header.ContentType.parse(ContentTypes.forName(file.getName)).toOption.toList*
              ),
              body = b
            )
          }
        case false => ZIO.succeed(Response.status(Status.NotFound))
      }
      .orDie

object FileServer:

  /**
   * Resolve `root` once: absolutise it, note whether it exists, and snapshot its real path for containment.
   */
  def apply(root: String): FileServer =
    val resolvedRoot = File(root).getAbsoluteFile
    val exists       = resolvedRoot.isDirectory
    val rootRealPath = if exists then resolvedRoot.toPath.toRealPath() else resolvedRoot.toPath
    new FileServer(resolvedRoot, exists, rootRealPath)
