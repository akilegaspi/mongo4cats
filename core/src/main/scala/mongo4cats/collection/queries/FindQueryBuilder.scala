/*
 * Copyright 2020 Kirill5k
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mongo4cats.collection.queries

import cats.effect.Async
import cats.syntax.functor._
import com.mongodb.ExplainVerbosity
import com.mongodb.client.model
import com.mongodb.reactivestreams.client.FindPublisher
import mongo4cats.bson.Document
import mongo4cats.helpers._
import mongo4cats.collection.operations
import mongo4cats.collection.operations.{Projection, Sort}
import org.bson.conversions.Bson

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

final case class FindQueryBuilder[F[_]: Async, T: ClassTag] private[collection] (
    private val observable: FindPublisher[T],
    private val commands: List[QueryCommand]
) extends QueryBuilder[FindPublisher, T] {

  /** Sets the maximum execution time on the server for this operation.
    *
    * @param duration
    *   the max time
    * @return
    *   FindQueryBuilder
    */
  def maxTime(duration: Duration): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.MaxTime(duration) :: commands)

  /** The maximum amount of time for the server to wait on new documents to satisfy a tailable cursor query. This only applies to a
    * TAILABLE_AWAIT cursor. When the cursor is not a TAILABLE_AWAIT cursor, this option is ignored.
    *
    * On servers &gt;= 3.2, this option will be specified on the getMore command as "maxTimeMS". The default is no value: no "maxTimeMS" is
    * sent to the server with the getMore command.
    *
    * On servers &lt; 3.2, this option is ignored, and indicates that the driver should respect the server's default value
    *
    * A zero value will be ignored.
    *
    * @param duration
    *   the max await time
    * @return
    *   the maximum await execution time in the given time unit
    */
  def maxAwaitTime(duration: Duration): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.MaxAwaitTime(duration) :: commands)

  /** Sets the collation options
    *
    * <p>A null value represents the server default.</p>
    *
    * @param collation
    *   the collation options to use
    * @return
    *   FindQueryBuilder
    * @since 1.3
    */
  def collation(collation: model.Collation): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.Collation(collation) :: commands)

  /** Get partial results from a sharded cluster if one or more shards are unreachable (instead of throwing an error).
    *
    * @param partial
    *   if partial results for sharded clusters is enabled
    * @return
    *   FindQueryBuilder
    */
  def partial(partial: Boolean): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.Partial(partial) :: commands)

  /** Sets the comment to the query. A null value means no comment is set.
    *
    * @param comment
    *   the comment
    * @return
    *   FindQueryBuilder
    * @since 1.6
    */
  def comment(comment: String): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.Comment(comment) :: commands)

  /** Sets the returnKey. If true the find operation will return only the index keys in the resulting documents.
    *
    * @param returnKey
    *   the returnKey
    * @return
    *   FindQueryBuilder
    * @since 1.6
    */
  def returnKey(returnKey: Boolean): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.ReturnKey(returnKey) :: commands)

  /** Sets the showRecordId. Set to true to add a field \$recordId to the returned documents.
    *
    * @param showRecordId
    *   the showRecordId
    * @return
    *   FindQueryBuilder
    * @since 1.6
    */
  def showRecordId(showRecordId: Boolean): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.ShowRecordId(showRecordId) :: commands)

  /** Sets the hint for which index to use. A null value means no hint is set.
    *
    * @param index
    *   the name of the index which should be used for the operation
    * @return
    *   FindQueryBuilder
    * @since 1.13
    */
  def hint(index: String): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.HintString(index) :: commands)

  /** Sets the hint for which index to use. A null value means no hint is set.
    *
    * @param hint
    *   the hint
    * @return
    *   FindQueryBuilder
    * @since 1.6
    */
  def hint(hint: Bson): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.Hint(hint) :: commands)

  /** Sets the exclusive upper bound for a specific index. A null value means no max is set.
    *
    * @param max
    *   the max
    * @return
    *   this
    * @since 1.6
    */
  def max(max: Bson): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.Max(max) :: commands)

  /** Sets the minimum inclusive lower bound for a specific index. A null value means no max is set.
    *
    * @param min
    *   the min
    * @return
    *   this
    * @since 1.6
    */
  def min(min: Bson): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.Min(min) :: commands)

  /** Sets the sort criteria to apply to the query.
    *
    * @param sort
    *   the sort criteria, which may be null.
    * @return
    *   FindQueryBuilder
    */
  def sort(sort: Bson): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.Sort(sort) :: commands)

  def sort(sorts: Sort): FindQueryBuilder[F, T] =
    sort(sorts.toBson)

  def sortBy(fieldNames: String*): FindQueryBuilder[F, T] =
    sort(Sort.asc(fieldNames: _*))

  def sortByDesc(fieldNames: String*): FindQueryBuilder[F, T] =
    sort(Sort.desc(fieldNames: _*))

  /** Sets the query filter to apply to the query.
    *
    * @param filter
    *   the filter
    * @return
    *   FindQueryBuilder
    */
  def filter(filter: Bson): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.Filter(filter) :: commands)

  def filter(filters: operations.Filter): FindQueryBuilder[F, T] =
    filter(filters.toBson)

  /** Sets a document describing the fields to return for all matching documents.
    *
    * @param projection
    *   the project document, which may be null.
    * @return
    *   FindQueryBuilder
    */
  def projection(projection: Bson): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.Projection(projection) :: commands)

  def projection(projection: Projection): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.Projection(projection.toBson) :: commands)

  /** Sets the number of documents to skip.
    *
    * @param skip
    *   the number of documents to skip
    * @return
    *   FindQueryBuilder
    */
  def skip(skip: Int): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.Skip(skip) :: commands)

  /** Sets the limit to apply.
    *
    * @param limit
    *   the limit
    * @return
    *   FindQueryBuilder
    */
  def limit(limit: Int): FindQueryBuilder[F, T] =
    FindQueryBuilder[F, T](observable, QueryCommand.Limit(limit) :: commands)

  def first: F[Option[T]] =
    applyCommands().first().asyncSingle[F].map(Option.apply)

  def all: F[Iterable[T]] =
    applyCommands().asyncIterable[F]

  def stream: fs2.Stream[F, T] =
    applyCommands().stream[F]

  def boundedStream(capacity: Int): fs2.Stream[F, T] =
    applyCommands().boundedStream[F](capacity)

  /** Explain the execution plan for this operation with the server's default verbosity level
    *
    * @return
    *   the execution plan
    * @since 4.2
    */
  def explain: F[Document] =
    applyCommands().explain().asyncSingle[F]

  /** Explain the execution plan for this operation with the given verbosity level
    *
    * @param verbosity
    *   the verbosity of the explanation
    * @return
    *   the execution plan
    * @since 4.2
    */
  def explain(verbosity: ExplainVerbosity): F[Document] =
    applyCommands().explain(verbosity).asyncSingle[F]

  override protected def applyCommands(): FindPublisher[T] =
    commands.reverse.foldLeft(observable) { case (obs, command) =>
      command match {
        case QueryCommand.ShowRecordId(showRecordId) => obs.showRecordId(showRecordId)
        case QueryCommand.ReturnKey(returnKey)       => obs.returnKey(returnKey)
        case QueryCommand.Comment(comment)           => obs.comment(comment)
        case QueryCommand.Collation(collation)       => obs.collation(collation)
        case QueryCommand.Partial(partial)           => obs.partial(partial)
        case QueryCommand.MaxTime(duration)          => obs.maxTime(duration.toNanos, TimeUnit.NANOSECONDS)
        case QueryCommand.MaxAwaitTime(duration)     => obs.maxAwaitTime(duration.toNanos, TimeUnit.NANOSECONDS)
        case QueryCommand.HintString(hint)           => obs.hintString(hint)
        case QueryCommand.Hint(hint)                 => obs.hint(hint)
        case QueryCommand.Max(index)                 => obs.max(index)
        case QueryCommand.Min(index)                 => obs.min(index)
        case QueryCommand.Skip(n)                    => obs.skip(n)
        case QueryCommand.Limit(n)                   => obs.limit(n)
        case QueryCommand.Sort(order)                => obs.sort(order)
        case QueryCommand.Filter(filter)             => obs.filter(filter)
        case QueryCommand.Projection(projection)     => obs.projection(projection)
        case QueryCommand.BatchSize(size)            => obs.batchSize(size)
        case QueryCommand.AllowDiskUse(allowDiskUse) => obs.allowDiskUse(allowDiskUse)
        case _                                       => obs
      }
    }
}
