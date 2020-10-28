package com.example.dailybingwallpapers.parsers

import android.util.Xml
import com.example.dailybingwallpapers.database.entities.BingImage
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.util.*

private val ns: String? = null
private val bingImageBaseUrlFormat = "https://www.bing.com{}_1920x1080.jpg"

class BingImageXmlParser {

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(xml: InputStream): List<*> {
        xml.use { xml ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(xml, null)
            parser.nextTag()
            return readImages(parser)

        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readImages(parser: XmlPullParser): List<BingImage> {
        val images = mutableListOf<BingImage>()

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

    private fun readImage(parser: XmlPullParser): BingImage {
        parser.require(XmlPullParser.START_TAG, ns, "image")
        var date: Date? = null
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

        
    }

    private fun skip(parser: XmlPullParser) {

    }

}