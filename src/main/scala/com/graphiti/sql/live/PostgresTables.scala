
package com.graphiti.sql.live

import java.sql.{PreparedStatement, ResultSet, Timestamp}

import com.graphiti.sql.DBComponent
import org.joda.time.DateTime
import slick.ast.NumericTypedType


case class Edge(id: String, edgeType: String, fromNodeId: String, toNodeId: String, createdAt: java.sql.Timestamp = new Timestamp(new java.util.Date().getTime()), modifiedAt: java.sql.Timestamp = new Timestamp(new java.util.Date().getTime()), properties: String = "{}", published: Boolean = false, deleted: Boolean = false, dirty: Boolean = false, checkpointId: Long = 1000L, lastPublishedAt: Option[java.sql.Timestamp] = None, filters: Option[String] = None)
case class Node(id: String, nodeType: String, createdAt: java.sql.Timestamp = new Timestamp(new java.util.Date().getTime()), modifiedAt: java.sql.Timestamp = new Timestamp(new java.util.Date().getTime()), properties: String = "{}", published: Boolean = false, dirty: Boolean = false, deleted: Boolean = false, checkpointId: Long = 1000L, lastPublishedAt: Option[java.sql.Timestamp] = None, filters: Option[String] = None)

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait PostgresTables { this: DBComponent =>

   import profile.api._

   val EST = java.util.TimeZone.getTimeZone("EST");
   val calendar = java.util.Calendar.getInstance(EST);
   val timestamp = new Timestamp(calendar.getTime().getTime)
  

  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = EdgeTable.schema ++ NodeTable.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table edge */

  /** GetResult implicit for fetching Edge objects using plain SQL queries */
  implicit def GetResultEdge(implicit e0: GR[String], e1: GR[java.sql.Timestamp], e2: GR[Boolean], e3: GR[Long], e4: GR[Option[java.sql.Timestamp]], e5: GR[Option[String]]): GR[Edge] = GR{
    prs => import prs._
    Edge.tupled((<<[String], <<[String], <<[String], <<[String], <<[java.sql.Timestamp], <<[java.sql.Timestamp], <<[String], <<[Boolean], <<[Boolean], <<[Boolean], <<[Long], <<?[java.sql.Timestamp], <<?[String]))
  }
  class EdgeTable(_tableTag: Tag) extends profile.api.Table[Edge](_tableTag, Some("live"), "edge") {
    def * = ((id, edgeType, fromNodeId, toNodeId, createdAt, modifiedAt, properties, published, deleted, dirty, checkpointId, lastPublishedAt, filters)).shaped.<> (
                     {tpl => import tpl._;Edge.apply(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13)},
                     {cls:Edge => Some(( cls.id, cls.edgeType, cls.fromNodeId, cls.toNodeId, cls.createdAt, cls.modifiedAt, cls.properties, cls.published, cls.deleted, cls.dirty, cls.checkpointId, cls.lastPublishedAt, cls.filters ))}
    )
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(edgeType), Rep.Some(fromNodeId), Rep.Some(toNodeId), Rep.Some(createdAt), Rep.Some(modifiedAt), Rep.Some(properties), Rep.Some(published), Rep.Some(deleted), Rep.Some(dirty), Rep.Some(checkpointId), lastPublishedAt, filters)).shaped.<> (
                     {tpl => import tpl._;Edge.apply(_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get, _10.get, _11.get, _12, _13)},
                     {(_:Any) =>  throw new Exception("Inserting into ? projection not supported.")}
    )

    /** Database column id SqlType(varchar), PrimaryKey, Length(64,true) */
    val id: Rep[String] = column[String]("id", O.SqlType("varchar"), O.PrimaryKey, O.Length(64,varying=true))
    /** Database column edge_type SqlType(varchar), Length(64,true) */
    val edgeType: Rep[String] = column[String]("edge_type", O.SqlType("varchar"), O.Length(64,varying=true))
    /** Database column from_node_id SqlType(varchar), Length(64,true) */
    val fromNodeId: Rep[String] = column[String]("from_node_id", O.SqlType("varchar"), O.Length(64,varying=true))
    /** Database column to_node_id SqlType(varchar), Length(64,true) */
    val toNodeId: Rep[String] = column[String]("to_node_id", O.SqlType("varchar"), O.Length(64,varying=true))
    /** Database column created_at SqlType(timestamp) */
    val createdAt: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("created_at", O.SqlType("timestamp"))
    /** Database column modified_at SqlType(timestamp) */
    val modifiedAt: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("modified_at", O.SqlType("timestamp"))
    /** Database column properties SqlType(jsonb), Length(2147483647,false) */
    val properties: Rep[String] = column[String]("properties", O.SqlType("jsonb"), O.Default("{}"))
    /** Database column published SqlType(bool), Default(false) */
    val published: Rep[Boolean] = column[Boolean]("published", O.SqlType("bool"), O.Default(false))
    /** Database column deleted SqlType(bool), Default(false) */
    val deleted: Rep[Boolean] = column[Boolean]("deleted", O.SqlType("bool"), O.Default(false))
    /** Database column dirty SqlType(bool), Default(false) */
    val dirty: Rep[Boolean] = column[Boolean]("dirty", O.SqlType("bool"), O.Default(false))
    /** Database column checkpoint_id SqlType(int8), Default(1000) */
    val checkpointId: Rep[Long] = column[Long]("checkpoint_id", O.SqlType("int8"), O.Default(1000L))
    /** Database column last_published_at SqlType(timestamp), Default(None) */
    val lastPublishedAt: Rep[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("last_published_at", O.SqlType("DATE"))
    /** Database column filters SqlType(_text), Length(2147483647,false), Default(None) */
    val filters: Rep[Option[String]] = column[Option[String]]("filters")

    /** Foreign key referencing NodeTable (database name edge_from_node_id_fkey) */
    lazy val nodeTableFk1 = foreignKey("edge_from_node_id_fkey", fromNodeId, NodeTable)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
    /** Foreign key referencing NodeTable (database name edge_to_node_id_fkey) */
    lazy val nodeTableFk2 = foreignKey("edge_to_node_id_fkey", toNodeId, NodeTable)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table EdgeTable */
  lazy val EdgeTable = new TableQuery(tag => new EdgeTable(tag))

  /** Entity class storing rows of table node */

  /** GetResult implicit for fetching Node objects using plain SQL queries */
  implicit def GetResultNode(implicit e0: GR[String], e1: GR[java.sql.Timestamp], e2: GR[Boolean], e3: GR[Long], e4: GR[Option[java.sql.Timestamp]], e5: GR[Option[String]]): GR[Node] = GR{
    prs => import prs._
    Node.tupled((<<[String], <<[String], <<[java.sql.Timestamp], <<[java.sql.Timestamp], <<[String], <<[Boolean], <<[Boolean], <<[Boolean], <<[Long], <<?[java.sql.Timestamp], <<?[String]))
  }
  class NodeTable(_tableTag: Tag) extends profile.api.Table[Node](_tableTag, Some("live"), "node") {
    def * = ((id, nodeType, createdAt, modifiedAt, properties, published, dirty, deleted, checkpointId, lastPublishedAt, filters)).shaped.<> (
                     {tpl => import tpl._;Node.apply(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11)},
                     {cls:Node => Some(( cls.id, cls.nodeType, cls.createdAt, cls.modifiedAt, cls.properties, cls.published, cls.dirty, cls.deleted, cls.checkpointId, cls.lastPublishedAt, cls.filters ))}
    )
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(nodeType), Rep.Some(createdAt), Rep.Some(modifiedAt), Rep.Some(properties), Rep.Some(published), Rep.Some(dirty), Rep.Some(deleted), Rep.Some(checkpointId), lastPublishedAt, filters)).shaped.<> (
                     {tpl => import tpl._;Node.apply(_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get, _10, _11)},
                     {(_:Any) =>  throw new Exception("Inserting into ? projection not supported.")}
    )

    /** Database column id SqlType(varchar), PrimaryKey, Length(64,true) */
    val id: Rep[String] = column[String]("id", O.SqlType("varchar"), O.PrimaryKey, O.Length(64,varying=true))
    /** Database column node_type SqlType(varchar), Length(64,true) */
    val nodeType: Rep[String] = column[String]("node_type", O.SqlType("varchar"), O.Length(64,varying=true))
    /** Database column created_at SqlType(timestamp) */
    val createdAt: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("created_at", O.SqlType("timestamp"))
    /** Database column modified_at SqlType(timestamp) */
    val modifiedAt: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("modified_at", O.SqlType("timestamp"))
    /** Database column properties SqlType(jsonb), Length(2147483647,false) */
    val properties: Rep[String] = column[String]("properties", O.SqlType("jsonb"), O.Default("{}"))
    /** Database column published SqlType(bool), Default(false) */
    val published: Rep[Boolean] = column[Boolean]("published", O.SqlType("bool"), O.Default(false))
    /** Database column dirty SqlType(bool), Default(false) */
    val dirty: Rep[Boolean] = column[Boolean]("dirty", O.SqlType("bool"), O.Default(false))
    /** Database column deleted SqlType(bool), Default(false) */
    val deleted: Rep[Boolean] = column[Boolean]("deleted", O.SqlType("bool"), O.Default(false))
    /** Database column checkpoint_id SqlType(int8), Default(1000) */
    val checkpointId: Rep[Long] = column[Long]("checkpoint_id", O.SqlType("int8"), O.Default(1000L))
    /** Database column last_published_at SqlType(timestamp), Default(None) */
    val lastPublishedAt: Rep[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("last_published_at", O.SqlType("DATE"))
    /** Database column filters SqlType(_text), Length(2147483647,false), Default(None) */
    val filters: Rep[Option[String]] = column[Option[String]]("filters")
  }
  /** Collection-like TableQuery object for table NodeTable */
  lazy val NodeTable = new TableQuery(tag => new NodeTable(tag))

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

  def allTables() = {
  	((tag:Tag)=> new NodeTable(tag))::
  	((tag:Tag)=> new EdgeTable(tag))::Nil
  }
}
