package com.schemagenerator.engine

import com.typesafe.config.ConfigFactory
import slick.SlickException
import slick.ast.ColumnOption
import slick.ast.ColumnOption.{AutoInc, Unique}
import slick.codegen.SourceCodeGenerator
import slick.model.Model
import slick.relational.RelationalProfile
import slick.relational.RelationalProfile.ColumnOption.{Default, Length}
import slick.sql.SqlProfile.ColumnOption.{NotNull, Nullable, SqlType}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


// The main application
object SchemaMappingCodeGenerator {

  def apply(xmlSchemaMapping: SchemaMapping, name:String,  model: Model): SourceCodeGenerator =
    new SchemaMappingCodeGenerator().getInstance(xmlSchemaMapping, name, model)

}

class SchemaMappingCodeGenerator[Code,TermName,TypeName] {

   var caseClasses:ListBuffer[String] = ListBuffer[String]()


  /** Slick code generator string extension methods. (Warning: Not unicode-safe, uses String#apply) */
  def getInstance(xmlSchemaMapping: SchemaMapping, name:String, model: Model): SourceCodeGenerator =

  // pass it to the Original Source code generator and
  new slick.codegen.SourceCodeGenerator(model) {


    /**
      * Generates code providing the data model as trait and object in a Scala package
      *
      * @group Basic customization overrides
      * @param profile Slick profile that is imported in the generated package (e.g. slick.driver.H2Driver)
      * @param pkg Scala package the generated code is placed in
      * @param container The name of a trait and an object the generated code will be placed in within the specified package.
      */
    override  def packageCode(profile: String, pkg: String, container: String, parentType: Option[String]) : String = {
    val pc =   s"""
package $pkg

import java.sql.{PreparedStatement, ResultSet, Timestamp}
import java.util.{Date}
import org.joda.time.DateTime
import slick.ast.NumericTypedType
import slick.jdbc.JdbcProfile

trait DBComponent {

  val profile: JdbcProfile
  val db: profile.api.Database

}

"""

val tbl = s"""
/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait ${name}{ this: DBComponent =>

   import profile.api._

   val EST = java.util.TimeZone.getTimeZone("EST");
   val calendar = java.util.Calendar.getInstance(EST);
   val timestamp = new Timestamp(calendar.getTime().getTime)
  ${indent(code)}
}
      """.trim()

      val cc =  s"""${caseClasses.mkString("\n")}

"""

        pc + cc + tbl
    }

    override def entityName = (dbName: String) => {

      //val table = xmlSchemaMapping.tables.find(_.table == dbName).get
      dbName.toCamelCase
    }

    override def tableName = (dbName: String) => {
      dbName.toCamelCase + "Table"
    }

    override def code = {

     s"""

${super.code}

class OptionalLongJdbcType extends profile.DriverJdbcType[Option[Long]] with NumericTypedType {
  def sqlType = java.sql.Types.BIGINT
  def setValue(v: Option[Long], p: PreparedStatement, idx: Int) = p.setLong(idx,v.getOrElse(0L))
  def getValue(r: ResultSet, idx: Int) = Some(r.getLong(idx))
  def updateValue(v: Option[Long], r: ResultSet, idx: Int) = r.updateLong(idx, v.getOrElse(0L))
}

lazy val optionalBooleanColumnTypeMapper =  new  profile.MappedJdbcType[Option[Boolean],Option[Long]]()(new OptionalLongJdbcType , scala.reflect.classTag[Option[Boolean]]){

    override def sqlType = java.sql.Types.BIGINT
    override def map(b:Option[Boolean]) = b match {
      case Some(true) => Some(1l)
      case Some(false) => Some(0l)
      case _ => None
    }
   override def comap(i: Option[Long]) = i match {
        case Some (1l) => Some (true)
        case Some (0l) => Some (false)
        case _ => None
   }
  override def getValue(r: ResultSet, idx: Int) = {
    val v = r.getLong(idx)
    if((v.asInstanceOf[AnyRef] eq null) || super.wasNull(r, idx)){
      None
    }
    else {
      v match {
        case  1l => Some(true)
        case  0l => Some(false)
        case  _ => None
      }
    }
  }
  override def wasNull(r: ResultSet, idx: Int) = {
    false
  }
  def setValue(v: Option[Long], p: PreparedStatement, idx: Int) = {
    if (v.isDefined) p.setLong(idx, v.get)
    else p.setNull(idx, sqlType)
  }
  def updateValue(v: Option[Long], r: ResultSet, idx: Int) = {
    if (v.isDefined)  r.updateLong(idx, v.get)
    else r.updateNull(idx)
  }
}

lazy val booleanColumnTypeMapper =  new  profile.MappedJdbcType[ Boolean , Long]()(longColumnType , scala.reflect.classTag[Boolean]){

  override def sqlType = java.sql.Types.BIGINT
  override def map(b:Boolean) = b match {
    case true => 1l
    case false => 0l
  }
  override def comap(i: Long) = i match {
    case 1l  => true
    case 0l  => false

  }
  override def getValue(r: ResultSet, idx: Int) = {
    val v = r.getLong(idx)
    if((v.asInstanceOf[AnyRef] eq null) || super.wasNull(r, idx)){
      false
    }
    else {
      v match {
        case  1l => true
        case  0l => false
      }
    }
  }
  override def wasNull(r: ResultSet, idx: Int) = {
    false
  }
   def setValue(v: Long, p: PreparedStatement, idx: Int) = {
     p.setLong(idx, v)
  }
  def updateValue(v: Long, r: ResultSet, idx: Int) = {
      r.updateLong(idx, v)
  }
}

lazy val longToStringColumnTypeMapper = MappedColumnType.base[Long, String](
  {l => l.toString},
  {s => s.toLong}
)
lazy val dateColumnTypeMapper = MappedColumnType.base[java.util.Date, java.sql.Timestamp](
  {d => new java.sql.Timestamp(d.getTime);},
  {t => new java.util.Date(t.getTime())}
)
lazy val timestamp2dateTime = MappedColumnType.base[DateTime, java.sql.Timestamp](
  dateTime => new java.sql.Timestamp(dateTime.getMillis),
  date => new DateTime(date)
)

def allTables() = {\n${model.tables.sortBy(t => t.foreignKeys.nonEmpty).map(t => s"\t((tag:Tag)=> new ${t.name.table.toCamelCase}Table(tag))").mkString("::\n")}${if(model.tables.size>0){s"::"}else{""}}Nil
}"""

    }

    override def Table = new Table(_) {

      val TUPLE_MAX_LENGTH = 22

      val Config = ConfigFactory.load()

      def isPloymorphicType = columns.size > TUPLE_MAX_LENGTH

      override def hlistEnabled =  false

      override def compoundType(types: Seq[String]): String = {
        if(hlistEnabled){
          def mkHList(types: List[String]): String = types match {
            case Nil => "HNil"
            case e :: tail => s"HCons[$e," + mkHList(tail) + "]"
          }
          mkHList(types.toList)
        }
        else compoundValue(types)
      }

      override def compoundValue(values: Seq[String]): String = {
        if(hlistEnabled) values.mkString(" :: ") + " :: HNil"
        else if (values.size == 1) values.head
        else  s"""(${values.mkString(", ")})"""
      }

      override def factory   = {
        if(columns.size == 1) TableClass.elementType.toString  else s"${TableClass.elementType}.tupled"
      }

      override def extractor = {
        s"${TableClass.elementType}.unapply"
      }

      override def  Index     = new Index(_){

        val table = xmlSchemaMapping.entities.find(_.table == model.table.table)

        override  def code = {
          val unique = if(model.unique) s", unique=true" else ""
          s"""val $name = index("$dbName", ${compoundValue(columns.map{case(c) =>{
            val property = if(table.isDefined) table.get.properties.flatten.find(_.column == c.model.name) else None
            val classAttributeName = if(property.isDefined)property.get.name else c.name
            classAttributeName
          }})}$unique)"""
        }
      }

      override def PrimaryKey = new  PrimaryKey(_){

        val table = xmlSchemaMapping.entities.find(_.table == model.table.table)
        val property = if(table.isDefined)table.get.properties.flatten.find(_.column == model.name) else None

        override def code = s"""val $name = primaryKey("$dbName", ${compoundValue(columns.map{case (c) =>{
          val classAttributeName = if(property.isDefined)property.get.name else c.name
          classAttributeName
        }})})"""
      }

      override def ForeignKey = new ForeignKey(_){

        override def code = {
          val pkTable = referencedTable.TableValue.name

          val (pkColumns, fkColumns) = (referencedColumns, referencingColumns).zipped.map { (p, f) =>
            val pTable = xmlSchemaMapping.entities.find(_.table == p.model.table.table)
            val pProperty = if(pTable.isDefined) pTable.get.properties.flatten.find(_.column == p.model.name) else None
            val pClassAttributeName = if(pProperty.isDefined)pProperty.get.name else p.name
            val pk = s"r.${pClassAttributeName}"

            val fTable = xmlSchemaMapping.entities.find(_.table == f.model.table.table)
            val fProperty = if(fTable.isDefined)fTable.get.properties.flatten.find(_.column == f.model.name) else None
            val fClassAttributeName = if(fProperty.isDefined)fProperty.get.name else f.name

            val fk = fClassAttributeName
            if(p.model.nullable && !f.model.nullable) (pk, s"Rep.Some($fk)")
            else if(!p.model.nullable && f.model.nullable) (s"Rep.Some($pk)", fk)
            else (pk, fk)
          }.unzip
          s"""lazy val $name = foreignKey("$dbName", ${compoundValue(fkColumns)}, $pkTable)(r => ${compoundValue(pkColumns)}, onUpdate=${onUpdate}, onDelete=${onDelete})"""
        }
      }

      override def  EntityType = new MyEntityTypeDef{}

      override def TableClass = new MyTableClassDef{}

      override def PlainSqlMapper = new MyPlainSqlMapperDef{}

      override def Column = new Column(_) {
        val columnName = model.name
        val isNullable = model.nullable
        val table = xmlSchemaMapping.entities.find(_.table == model.table.table)
        val property = if(table.isDefined)table.get.properties.flatten.find(_.column == model.name) else None
        val isDefined = if (property.isDefined) property.get.hbmType.isDefined else false
        val defaultValueSqlType = model.options.headOption.get
        val defaultValueScalaType = model.tpe
        val NumberPattern = """(.*)([0-9]+)(.*)""".r

        override def default: Option[String] = model.options.collect{
          case RelationalProfile.ColumnOption.Default(Some(value)) if( defaultValueScalaType == "scala.math.BigDecimal" ) => {
              if (isDefined) {
                Some(value.asInstanceOf[BigDecimal].toInt == 1)
              } else {
                Some(value.asInstanceOf[BigDecimal].toLong)
              }
          }
          case RelationalProfile.ColumnOption.Default(None) => None
          case RelationalProfile.ColumnOption.Default(value) if( defaultValueScalaType == "scala.math.BigDecimal" ) => {
              if (isDefined) {
                value.asInstanceOf[BigDecimal].toInt == 1
              } else {
                value.asInstanceOf[BigDecimal].toLong
              }
          }
          case RelationalProfile.ColumnOption.Default(value) => value
          case _ if asOption => None
        }.map(defaultCode).headOption

        //  def rawType: Code = parseType(model.tpe)
        override def rawType: scala.Predef.String = {

          val modelType = parseType(model.tpe)

          modelType match {
            case "scala.math.BigDecimal" =>
              model.options.headOption.get match {
                case SqlType("NUMBER") => if(property.isDefined)property.get.hbmType.getOrElse("Long") else "Long"
                case _ => modelType
              }
            case "Clob" =>
              model.options.headOption.get match {
                case SqlType("CLOB") =>  if(property.isDefined)property.get.hbmType.getOrElse("String") else "Long"
                case _ => modelType
              }
            case "java.sql.Clob" =>
              model.options.headOption.get match {
                case SqlType("CLOB") =>  if(property.isDefined)property.get.hbmType.getOrElse("String") else "Long"
                case _ => modelType
              }
            case "String" =>
              model.options.headOption.get match {
                case SqlType("VARCHAR") | SqlType("VARCHAR2")|SqlType("LONGTEXT") =>  if(property.isDefined)property.get.hbmType.getOrElse("String") else "String"
                case SqlType("jsonb") =>{
                  modelType
                }
                case _ => modelType
              }
            case _ => modelType
          }
        }

         /** Generates code for the ColumnOptions (DBType, AutoInc, etc.) */
         override def options: Iterable[String] =
         model.options.filter{
          //case t: SqlProfile.ColumnOption.SqlType => dbType
          case _ => true
        }.flatMap(columnOptionCode(_).toSeq)

        override  def columnOptionCode = { code =>
          code match{
                case ColumnOption.PrimaryKey => Some(s"O.PrimaryKey")
                case Default(value) => {
                  model.options.headOption.get match {
                    case SqlType("jsonb") => {
                      Some(s"""O.Default("{}")""")
                    }
                    case SqlType("timestamp") => {
                      Some(s"""O.Default("now()")""")
                    }
                    case _ => Some(s"O.Default(${default.get})")
                  }
                } // .get is safe here
                case SqlType(dbType) => {
                      Some(s"""O.SqlType("$dbType")""")
                }
                case Length(length, varying) => {
                  model.options.headOption.get match {
                    case SqlType("jsonb") => {
                      Some(s"""O.Default("{}")""")
                    }
                    case _ => Some(s"O.Length($length,varying=$varying)")
                  }
                }
                case AutoInc => Some(s"O.AutoInc")
                case Unique => Some(s"O.Unique")
                case NotNull | Nullable => throw new SlickException(s"Please don't use Nullable or NotNull column options. Use an Option type, respectively the nullable flag in Slick's model model Column.")
                case o => None // throw new SlickException( s"Don't know how to generate code for unexpected ColumnOption $o." )
          }
        }


        override def code = {

          val table = xmlSchemaMapping.entities.find(_.table == model.table.table)
          val id = if(table.isDefined && table.get.ids.isDefined)table.get.ids.get.find(_.column == model.name) else None
          val o =  if(id.isDefined) options ++ Seq(s"PrimaryKey")  else options
          val colName = columnName
          val isNullable = model.nullable

          val v = o.map(", " + _).mkString("")
          val opt = if (v == "" || model.nullable) {
            model.tpe match {
              case "java.sql.Timestamp" => """, O.SqlType("DATE")"""
              case "scala.math.BigDecimal" => """, O.SqlType("NUMBER")"""
              case "java.sql.Clob" => """, O.SqlType("CLOB")"""
              case _ => ""
            }
          } else v


          val propertyVal = if(property.isDefined)property.get.hbmType.getOrElse("").capitalize
          val actualVal = model.tpe.split('.').last.capitalize
          val mapper =   propertyVal match {
            case "Boolean" =>{
              actualVal match {
                case "BigDecimal" =>{
                  property.get.reversed match {
                    case Some(true)  =>
                      "(inverseOptionalBooleanColumnTypeMapper)"
                    case _ =>
                     if(isNullable) "(optionalBooleanColumnTypeMapper)" else "(booleanColumnTypeMapper)"
                  }
                }
                case _ =>""
              }
            }
            case "Long" =>{
              actualVal match {
                case  "String" =>{
                  "(longToStringColumnTypeMapper)"
                }
                case _ => ""
              }
            }
            case _ => ""
          }
          //DEBUG: add ability to change column name as specified in xml
          val classAttributeName = if(property.isDefined)property.get.name else name
          val nullable = if(mapper.nonEmpty && model.nullable)true else false

          s"""val $classAttributeName: Rep[${if( propertyVal == "Boolean"){if(isNullable) "Option[Boolean]" else "Boolean"} else actualType}] = column[${
            if( propertyVal == "Boolean" ) {
              if(isNullable) "Option[Boolean]" else "Boolean"
            }else if(mapper.nonEmpty && model.nullable) {
              rawType
            }else actualType}]("${model.name}"$opt)$mapper"""

        }

        override def defaultCode = {
          case Some(v) => s"Some(${defaultCode(v)})"
          case None      => s"None"
          case v:String  => "\""+v+"\""
          case v:Byte    => s"$v"
          case v:Int     => s"$v"
          case v:Long    => s"${v}L"
          case v:Float   => s"${v}F"
          case v:Double  => s"$v"
          case v:Boolean => s"$v"
          case v:Short   => s"$v"
          case v:Char   => s"'$v'"
          case v => throw new SlickException( s"Don't know how to generate code for default value $v of ${v.getClass}. Override def defaultCode to render the value." )
        }

      }

      trait MyEntityTypeDef extends EntityTypeDef{

        val table = xmlSchemaMapping.entities.find(_.table == model.name.table)

        override def doc = {
          s"Entity class storing rows of table ${model.name.table}\n"
        }
        override def code = {
val result  =
          if(isPloymorphicType){


            val m = columns.zipWithIndex.groupBy (_._1.actualType)
            val columnsByType = mutable.LinkedHashMap(m.toSeq sortBy (_._2.head._2): _*)
            val typeNames = columnsByType.keys.toVector

            val list = for{
              (column,columnPosition) <- columns.zipWithIndex
            }yield{
              val actualType = column.actualType
              val index = typeNames.indexOf(actualType)
              ( columnPosition, index)
            }
            /**
            val methodArgTpeList = columnsByType.zipWithIndex.map(c =>{s"M${c._2}, U${c._2}, P${c._2}"} )
            val methodArgList = for {(id, tpeListPosIdx) <- list} yield s"s$id:Shape[_ <: Level,M$tpeListPosIdx, U$tpeListPosIdx, P$tpeListPosIdx]"

            s"""\nobject $name{
  def apply(attributes:Seq[Any]) = new ${name}(attributes)
  implicit def convertToShaped[Level <: ShapeLevel, ${methodArgTpeList.mkString(", ")} ](implicit ${methodArgList.mkString(", ")})={\n\t\tnew ${name}Shape[Level,${
              (for{(letter) <- List("M", "U", "P") }yield s"$name[${
                columnsByType.zipWithIndex.map(col => {
                  s"$letter${col._2}"
                }).mkString(",")
              }]").mkString(",")}](Seq(${
              columns.zipWithIndex.map{ case  (_,i)=> s"s$i"}.mkString(",")}))\n\t}\n}

${val typeDeclaration = s"$name[${columnsByType.map(_=>s"_").mkString(",")}]"
              s"""final class ${name}Shape[Level <: ShapeLevel,M <: $typeDeclaration, U <: $typeDeclaration : ClassTag, P <: $typeDeclaration](val shapes: Seq[Shape[_ <: ShapeLevel, _, _, _]]) extends MappedScalaProductShape[Level, $typeDeclaration, M, U, P] {"""
            }
  def copy(shapes: Seq[Shape[_ <: ShapeLevel, _, _, _]]) = new ${name}Shape(shapes)
  def buildValue(elems: IndexedSeq[Any]) = {\n\t\t$name (${columns.zipWithIndex.map{ case  (_,i)=> s"elems($i)"}.mkString(", ")})
  }
}*/
            s"""${
              val constructorArgTpeList = columnsByType.zipWithIndex.map(c => if(c._1._1.startsWith("Option")){s"+M${c._2}"} else {s"M${c._2}"})
              val constructorArgList = columns.map { case (c) =>

                val property = if(table.isDefined)table.get.properties.flatten.find(_.column == c.model.name) else None
                val classAttributeName = if(property.isDefined)property.get.name else c.name

                c.default.map(v =>
                  s"${classAttributeName}: M${typeNames.indexOf(c.actualType)} = $v"
                ).getOrElse(
                  s"${classAttributeName}: M${typeNames.indexOf(c.actualType)}"
                )
              }.mkString(", ")

              val castArgList = columns.zipWithIndex.map{case (c,index)=> s"args($index).asInstanceOf[M${typeNames.indexOf(c.actualType)}]"}.mkString(", ")

              val prns = (parents.take(1).map(" extends "+_) ++ parents.drop(1).map(" with "+_)).mkString("")
              s"""case class $name[${constructorArgTpeList.mkString(",")}]($constructorArgList)$prns{
  def this(args: Seq[Any]) = {
    this( $castArgList)
  }
}""".stripMargin
            }"""

          }
          else {
            //super.code

            val args = columns.map { case (c) =>

              val property = if(table.isDefined)table.get.properties.flatten.find(_.column == c.model.name) else None
              val classAttributeName = if(property.isDefined)property.get.name else c.name

              c.default.map(v =>
                s"${classAttributeName}: ${c.exposedType} = $v"
              ).getOrElse(
                s"${

                    if(c.exposedType == "String" && classAttributeName == "properties"){
                      s"""${classAttributeName}: ${c.exposedType} = "{}""""
                    }else if(c.exposedType == "java.sql.Timestamp"){
                      s"${classAttributeName}: ${c.exposedType} = new Timestamp(new java.util.Date().getTime())"
                    }
                    else s"${classAttributeName}: ${c.exposedType}"


                }"
              )
            }.mkString(", ")
            if(classEnabled){
              val prns = (parents.take(1).map(" extends "+_) ++ parents.drop(1).map(" with "+_)).mkString("")
              s"""case class $name($args)$prns"""
            } else {
              s"""
type $name = $types
/** Constructor for $name providing default values if available in the database schema. */
def $name($args): $name = {
  ${compoundValue(columns.map(_.name))}
}
          """.trim
            }

          }
 caseClasses +=  result
          ""
        }
      }//end of MyEntityTypeDef

      trait MyTableValueDef extends TableValueDef{
        override def code = {
          s"lazy val ${name}QueryInstance = new TableQuery(tag => new ${TableClass.name}(tag))"
        }

      }//end of MyTableValueDef

      trait MyPlainSqlMapperDef extends PlainSqlMapperDef{

        override def code = {

          if(isPloymorphicType){

            val positional = compoundValue(columnsPositional.map(c => if (c.fakeNullable || c.model.nullable) s"<<?[${c.rawType}]" else s"<<[${c.rawType}]"))
            val m = columns.zipWithIndex.groupBy (_._1.actualType)
            val columnsByType = mutable.LinkedHashMap(m.toSeq sortBy (_._2.head._2): _*)
            val dependencies = columnsByType.zipWithIndex.map{ case (c,i) => s"""e$i: GR[${c._2.head._1.actualType}]"""}.mkString(", ")
            val rearranged = compoundValue(desiredColumnOrder.map(i => if(hlistEnabled) s"r($i)" else tuple(i)))
            def result(args: String) =  if(isPloymorphicType)s"${TableClass.elementType}$args" else if(mappingEnabled) s"$factory($args)" else args

            val body = if(autoIncLastAsOption && columns.size > 1){
              s"""
                            val r = $positional
                            import r._
                            ${result(rearranged)} // putting AutoInc last
                          """.trim
            } else{
              result(positional)
            }

            val models  = columns.map(c => if (c.fakeNullable || c.model.nullable) c.model.copy(tpe = s"Option[${c.model.tpe}]") else c.model)
            val typeList =  columnsByType.keys

            s"""
                  implicit def $name(implicit $dependencies): GR[${TableClass.elementType}${if(isPloymorphicType && typeList.nonEmpty)s"[${typeList.mkString(", ")}]"}] = GR{\n\tprs => import prs._\n\t${indent(body)}\n}""".trim
          }
          else{
            super.code
          }
        }
      }//end of MyPlainSqlMapperDef

      trait MyTableClassDef extends TableClassDef{

        val table = xmlSchemaMapping.entities.find{case(currentMapping) =>{  currentMapping.table ==  model.name.table }}

        override  def doc: String ={
          ""
        }
        override def star = {

          if(isPloymorphicType){

            val select = columns.map{case (c)=>{
              val property = if(table.isDefined)table.get.properties.flatten.find(_.column == c.model.name) else None
              val classAttributeName = if(property.isDefined)property.get.name else c.name
              s"$classAttributeName"
            }}.mkString(" :: ") + " :: HNil"

            val struct = columns.map{case (c)=>{
              val property = if(table.isDefined)table.get.properties.flatten.find(_.column == c.model.name) else None
              val classAttributeName = if(property.isDefined)property.get.name else c.name
              val mappedPropertyTpe = if(property.isDefined)property.get.hbmType.getOrElse("").capitalize


              //if(mappedPropertyTpe == "Boolean" && c.model.nullable == false)s"Some(s.$classAttributeName)" else s"s.$classAttributeName"
              s"s.$classAttributeName"
            }}.mkString(" :: ") + " :: HNil"


            val rhs = if(mappingEnabled) {

              val m = columns.zipWithIndex.groupBy (_._1.actualType)
              val columnsByType = mutable.LinkedHashMap(m.toSeq sortBy (_._2.head._2): _*)
              val typeList = columnsByType.keys.mkString(", ")


              val elems = columns.zipWithIndex.map{ case(c, i) => {
                val property = if(table.isDefined)table.get.properties.flatten.find(_.column == c.model.name) else None
                val classAttributeName = if(property.isDefined)property.get.name else c.name
                val mappedPropertyTpe = if(property.isDefined)property.get.hbmType.getOrElse("").capitalize

                s"elems($i)"
                  //${if(mappedPropertyTpe == "Boolean" && c.model.nullable == false)".get" else ""}"

              }}mkString(", ")

              s"""($select).shaped.<> (
                 {case elems:HList => ${TableClass.elementType}($elems)},
                 {(b:${TableClass.elementType}${if (typeList.nonEmpty) s"[$typeList]"})  => b match { case (s:${TableClass.elementType}${if (typeList.nonEmpty) s"[$typeList]"}) => Option($struct)}}
)""".stripMargin
            } else {
              select
            }
            s"""def * = {
               $rhs
              }""".stripMargin
          }
          else {
            //super.star
            val struct = compoundValue(columns.map{case (c)=>{
              val property = if(table.isDefined)table.get.properties.flatten.find(_.column == c.model.name) else None
              val classAttributeName = if(property.isDefined)property.get.name else c.name
              s"$classAttributeName"
            }})

            val rhs = if(mappingEnabled){

              val indexes = columns.zipWithIndex.map{ case(c, i) => {
                val property = if(table.isDefined)table.get.properties.flatten.find(_.column == c.model.name) else None
                val mappedPropertyTpe = if(property.isDefined)property.get.hbmType.getOrElse("").capitalize
                val actualTpe = c.model.tpe.split('.').last.capitalize

                //if (mappedPropertyTpe == "Boolean" && c.model.nullable == false) s"_${i+1}.get" else  s"_${i+1}"}
                s"_${i+1}"}
              }.mkString(", ")
              val values = columns.zipWithIndex.map{ case(c, i) => {
                val property = if(table.isDefined)table.get.properties.flatten.find(_.column == c.model.name) else None
                val mappedPropertyTpe = if(property.isDefined)property.get.hbmType.getOrElse("").capitalize
                val classAttributeName = if(property.isDefined)property.get.name else c.name
                val actualTpe = c.model.tpe.split('.').last.capitalize

                //if (mappedPropertyTpe == "Boolean" && c.model.nullable == false) s"Some(cls.${classAttributeName})" else s"cls.${classAttributeName}"}
                s"cls.${classAttributeName}"}
              }.mkString(", ")
              s"""($struct).shaped.<> (
                 {tpl => ${if (indexes != "_1") s"import tpl._;${TableClass.elementType}.apply($indexes)" else s"${TableClass.elementType}.apply(tpl)"}},
                 {cls:${TableClass.elementType} => Some(( $values ))}
)""".stripMargin
            }
            else {
              struct
            }
            s"def * = $rhs"
          }
        }
        override def option = {

          def optFactory = {

            val accessors = columns.zipWithIndex.map{ case(c,i) =>
              val accessor = if(columns.size > 1) tuple(i) else "r"
              if(c.asOption || c.model.nullable) accessor else s"$accessor.get"
            }
            val fac = s"$factory(${compoundValue(accessors)})"
            val discriminator = columns.zipWithIndex.collect{ case (c,i) if !c.model.nullable => if(columns.size > 1) tuple(i) else "r" }.headOption
            val expr = discriminator.map(d => s"$d.map(_=> $fac)").getOrElse(s"None")
            if(columns.size > 1)
              s"{r=>import r._; $expr}"
            else
              s"r => $expr"

          }

          if (isPloymorphicType) {


            val select = columns.map { case (c) => {
              val property = if (table.isDefined) table.get.properties.flatten.find(_.column == c.model.name) else None
              val classAttributeName = if (property.isDefined) property.get.name else c.name
              if (c.model.nullable) s"${classAttributeName}" else s"Rep.Some(${classAttributeName})"
            }
            }.mkString(" :: ") + " :: HNil"

            val elems = columns.zipWithIndex.map{ case(c, i) => {
              val property = if(table.isDefined)table.get.properties.flatten.find(_.column == c.model.name) else None
              val classAttributeName = if(property.isDefined)property.get.name else c.name
              val mappedPropertyTpe = if(property.isDefined)property.get.hbmType.getOrElse("").capitalize

              if(c.asOption || c.model.nullable) s"elems($i)" else s"elems($i).get"


            }}mkString(", ")

            val rhs = if (mappingEnabled)
              s"""($select).shaped.<>(
                 |{case elems:HList => ${TableClass.elementType}($elems)},
                 |{(_:Any) =>  throw new Exception("Inserting into ? projection not supported.")})""".stripMargin else select
            s"def ? = $rhs"
          }



          else{
            //super.option
            val struct = compoundValue(columns.map{case (c)=>{
              val property = if(table.isDefined)table.get.properties.flatten.find(_.column == c.model.name) else None
              val classAttributeName = if(property.isDefined)property.get.name else c.name
              if(c.model.nullable) s"$classAttributeName" else s"Rep.Some(${classAttributeName})"
            }})
            /**
              * compoundValue(columns.map(c=>if(c.model.nullable)s"${c.name}" else s"Rep.Some(${c.name})"))
              */
            val rhs = if(mappingEnabled){

              val indexes = columns.zipWithIndex.map{ case(c, i) => {
                val property = if(table.isDefined)table.get.properties.flatten.find(_.column == c.model.name) else None
                val mappedPropertyTpe = if(property.isDefined)property.get.hbmType.getOrElse("").capitalize
                val actualTpe = c.model.tpe.split('.').last.capitalize

                //if (mappedPropertyTpe == "Boolean" && c.model.nullable == false) s"_${i+1}.get" else  s"_${i+1}"}
                if(columns.size > 1){
                  s"_${i+1}${if(c.model.nullable){""}else{".get"}}"
                }else{
                  s"tpl${if(c.model.nullable){""}else{".get"}}"
                }

              }
              }.mkString(", ")
              s"""($struct).shaped.<> (
                 {tpl => ${if (columns.size > 1) {s"import tpl._;${TableClass.elementType}.apply($indexes)"} else s"${TableClass.elementType}.apply($indexes)"}},
                 {(_:Any) =>  throw new Exception("Inserting into ? projection not supported.")}
)""".stripMargin}
            else {
              struct
            }


            s"def ? = $rhs"
          }
        }
        override def optionFactory = {

          if(isPloymorphicType){

            val accessors = columns.zipWithIndex.map{ case(c,i) =>
              val accessor = "r"
              if(c.fakeNullable || c.model.nullable) accessor else s"r.get"
            }
            val fac = s"${TableClass.elementType.toString}${compoundValue(accessors)}"
            val discriminator = columns.zipWithIndex.collect{ case (c,i)=> "r" }.headOption
            val expr = discriminator.map(d => s"$d($fac)").getOrElse(s"None")

            s"r => $expr"

          }
          else  {

            val accessors = columns.zipWithIndex.map { case (c, i) =>
              val accessor = if (columns.size > 1) tuple(i) else "r"
              if (c.fakeNullable || c.model.nullable) accessor else s"$accessor.get"
            }
            val fac = s"$factory(${compoundValue(accessors)})"
            val discriminator = columns.zipWithIndex.collect { case (c, i) if !c.model.nullable => if (columns.size > 1) tuple(i) else "r" }.headOption
            val expr = discriminator.map(d => s"$d.map(_=> $fac)").getOrElse(s"None")
            if (columns.size > 1)
              s"{r=>import r._; $expr}"
            else
              s"r => $expr"
          }
        }
        override def code = {
          if (isPloymorphicType) {

            val prns = parents.map(" with " + _).mkString("")
            val args = model.name.schema.map(n => s"""Some("$n")""") ++ Seq("\"" + model.name.table + "\"")
            val models = columns.map(c => if (c.fakeNullable || c.model.nullable) c.model.copy(tpe = s"Option[${c.model.tpe}]") else c.model)
            val m = columns.zipWithIndex.groupBy (_._1.actualType)
            val columnsByType = mutable.LinkedHashMap(m.toSeq sortBy (_._2.head._2): _*)
            val typeList = columnsByType.keys.mkString(", ")

            s"""
class $name(_tableTag: Tag) extends Table[$elementType ${if (typeList.nonEmpty) s"[$typeList]"}](_tableTag, ${args.mkString(", ")})$prns {
    ${indent(body.map(_.mkString("\n")).mkString("\n\n"))}
}
        """.trim()
          }
          else {
            super.code
          }
        }


      }//end of MyTableClassDef


    }// end of def table



  }// end of source code generator
}

