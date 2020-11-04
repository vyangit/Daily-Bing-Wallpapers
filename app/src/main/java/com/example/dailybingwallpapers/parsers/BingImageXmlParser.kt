package com.example.dailybingwallpapers.parsers

import android.media.Image
import android.util.Xml
import com.example.dailybingwallpapers.storage.database.entities.BingImage
import com.example.dailybingwallpapers.network.dto.BingImageMetaDataDTO
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

private val ns: String? = null
private const val bingImageBaseUrlFormat = "https://www.bing.com%s_1920x1080.jpg"

class BingImageXmlParser {

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(xml: InputStream): List<BingImageMetaDataDTO> {
        xml.use { xml ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(xml, null)
            parser.nextTag()
            return readImages(parser)

        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readImages(parser: XmlPullParser): List<BingImageMetaDataDTO> {
        val images = mutableListOf<BingImageMetaDataDTO>()

        parser.require(XmlPullParser.START_TAG, ns, "images")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            if (parser.name == "image") {
                images.add(readImage(parser))
            } else {
                skip(parser)
            }
        }

        return images
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readImage(parser: XmlPullParser): BingImageMetaDataDTO {
        parser.require(XmlPullParser.START_TAG, ns, "image")
        var date: LocalDate? = null
        var imageUrl: String? = null
        var copyright: String? = null
        var copyrightLink: String? = null
        var headline: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                "startdate" -> date = readDate(parser)
                "urlBase" -> imageUrl = readImageUrl(parser)
                "copyright" -> copyright = readCopyright(parser)
                "copyrightlink" -> copyrightLink = readCopyrightLink(parser)
                "headline" -> headline = readHeadline(parser)
                else -> skip(parser)
            }
        }

        return BingImageMetaDataDTO(
            date!!,
            imageUrl!!,
            copyright!!,
            copyrightLink!!,
            headline!!
        )
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readDate(parser: XmlPullParser): LocalDate {
        parser.require(XmlPullParser.START_TAG, ns, "startdate")
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ENGLISH)
        val date = LocalDate.parse(readText(parser), formatter)
        parser.require(XmlPullParser.END_TAG, ns, "startdate")
        return date
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readImageUrl(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "urlBase")
        val imageUrl = bingImageBaseUrlFormat.format(readText(parser))
        parser.require(XmlPullParser.END_TAG, ns, "urlBase")
        return imageUrl
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readCopyright(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "copyright")
        val copyright = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "copyright")
        return copyright
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readCopyrightLink(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "copyrightlink")
        val copyrightLink = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "copyrightlink")
        return copyrightLink
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readHeadline(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "headline")
        val headline = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "headline")
        return headline
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}