package com.schemagenerator.engine

import scala.collection.mutable.ListBuffer
import scala.xml._
import scala.xml.factory.XMLLoader
import scala.xml.transform.{RewriteRule, RuleTransformer}


object MyXMLLoader extends XMLLoader[Elem] {

  override def parser: SAXParser = {
    val f = javax.xml.parsers.SAXParserFactory.newInstance()
    f.setValidating(false)
    f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    f.newSAXParser()
  }
}

case class PrimaryKey(name: String, column: String )

case class Property(name: String, column: String, hbmType: Option[String], reversed: Option[Boolean])

case class Entity(scalaClassName: String, schema: String, table: String, properties: Seq[Option[Property]], ids:Option[Seq[PrimaryKey]])

case class SchemaMapping(schema: String, name:String, entities: Seq[Entity])

object XmlBuilder{

  val canonicalPath = new java.io.File(".").getCanonicalPath + "/src/main/resources/"

  def apply(fileName: String): List[SchemaMapping] = new XmlBuilder(canonicalPath+fileName).build
}

class XmlBuilder(name: String) {

  def hbmXML = MyXMLLoader.loadFile(name) \\ "mappings"

  lazy val transformer = new RuleTransformer(
    new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case e : Elem  if (e.attributes.exists(_.key == "schema"))=> {
          val textNode = Text(n.\("@schema").text)
          val attr = Attribute(None, "schema", textNode, scala.xml.Null)
          e.%(attr)
        }
        case e : Elem  if (e.attributes.exists(_.key == "table") & !e.attributes.exists(_.key == "schema")) =>{
          val parent = (hbmXML.head \\ "_").filter(p=> p.attribute("table").isDefined & p.attribute("schema").isDefined & (p \ "_").exists(c=>c.equals(e)))
          val txt = parent.\("@schema").text
          //HACK START: THIS SHOULD NEVER HAPPEN !!! if the immediate parent does not have a schema attribute use the one from the root node
          val v = if(txt == "")hbmXML.head.attribute("schema").get.text else txt
          //HACK END
          val textNode = Text(v)
          val attr = Attribute(None, "schema", textNode, scala.xml.Null)
          e.%(attr)
        }
        case other => other
      }
    }
  )


  def build(): List[SchemaMapping] = {

    val xmlNodeList = flatten(transformer.transform(hbmXML.head).head)

    val xmlNodesBySchema = xmlNodeList.filter((n: Node) => n.attribute("table").isDefined & n.attribute("schema").isDefined).groupBy(_.attribute("schema").get.text)

   val result =  xmlNodesBySchema.keys.map((key) => {

    val entities = xmlNodesBySchema(key).map((n: Node) => {

    val eName = if (n.\("@name").isEmpty) None else Some(n.\("@name").text.toSimpleClassName)
    val eSchema = n.\@("schema")
    val eTable = n.\@("table")

    Entity(eName.getOrElse(eTable), eSchema toUpperCase, eTable toUpperCase, mappableColumns(n),pKColumns(n))

      })
      val dups = entities.groupBy(entity => entity.scalaClassName).values.filter(p => p.size > 1).toList.flatten

      val finalList = entities.map(entity => if (dups.contains(entity)) entity.copy(scalaClassName = entity.scalaClassName + "_" + dups.indexOf(entity)) else entity)

       SchemaMapping(key,"Graphiti", finalList)

    }).toList

    result
  }

  def flatten(node: Node): List[Node] = {

    node :: node.child.flatMap( child => flatten(child) ).toList
  }

  def mappableColumns = (nodes: NodeSeq) => {

    val properties = ListBuffer[Option[Property]]()

    nodes.foreach((node: Node) => {

      "id property many-to-many many-to-one one-to-many".split(" ").foreach { prop =>

        (node\prop).foreach { child =>

          properties += Some(Property(
            if(child.\@("name").isEmpty)child.\@("column").toCamelCase.unCapitalize else child.\@("name")
            ,
            child.\@("column").toUpperCase,
            //these types are generated by default, no need to specify
            if (child.\@("type").isEmpty() | ("int timestamp integer java.lang.integer date java.util.date").split(" ").contains(child.\@("type").toLowerCase())){
              None
            }
            else {
              val str = child.\@("type").split('.').last
              Some(str.capitalize)
            },
            if (child.\@("reversed").isEmpty()) {
              None
            } else {
              val boleanValue = child.\@("reversed").toLowerCase == "true"
              Some(boleanValue)
            }
          ))
        }
      }
    })
    properties

  }
  def pKColumns = (nodes: NodeSeq) => {

    val ids = ListBuffer[PrimaryKey]()

    nodes.foreach((node: Node) => {

      (node \ "composite-id" \ "key-property").foreach{ child =>

        ids += PrimaryKey(
          if(child.\@("name").isEmpty)child.\@("column").toCamelCase.unCapitalize else child.\@("name"),
          child.\@("column").toUpperCase
        )
      }

      "id key".split(" ").foreach { prop =>

        (node\prop).foreach { child =>

          ids += PrimaryKey(
            if(child.\@("name").isEmpty)child.\@("column").toCamelCase.unCapitalize else child.\@("name"),
            child.\@("column").toUpperCase
          )
        }
      }

    })

    if(ids.size>0)Some(ids)else None

  }


}

