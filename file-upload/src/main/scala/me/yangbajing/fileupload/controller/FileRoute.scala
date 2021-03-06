package me.yangbajing.fileupload.controller

import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.StrictLogging
import me.yangbajing.fileupload.service.FileService
import me.yangbajing.fileupload.util.FileUtils

class FileRoute(fileService: FileService) extends StrictLogging {

  def route: Route = pathPrefix("file") {
    log {
      uploadRoute ~
        downloadRoute ~
        progressRoute
    }
  }

  private def uploadRoute: Route = path("upload") {
    post {
      withoutSizeLimit {
        entity(as[Multipart.FormData]) { formData =>
          onSuccess(fileService.handleUpload(formData)) { results =>
            import me.yangbajing.fileupload.util.JacksonSupport._
            complete(results)
          }
        }
      }
    }
  }

  // 支持断点续传
  private def downloadRoute: Route = path("download" / Segment) { hash =>
    getFromFile(FileUtils.getLocalPath(hash).toFile)
  }

  // 查询文件上传进度
  private def progressRoute: Route = path("progress" / Segment) { hash =>
    onSuccess(fileService.progressByHash(hash)) {
      case Some(v) =>
        import me.yangbajing.fileupload.util.JacksonSupport._
        complete(v)
      case None => complete(StatusCodes.NotFound)
    }
  }

  private val log: Directive0 = extractRequest.flatMap { req =>
    logger.debug(req.toString())
    pass
  }

}
