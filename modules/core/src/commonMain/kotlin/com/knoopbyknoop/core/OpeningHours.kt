package com.knoopbyknoop.core

/** A moment in a ferry's local time: [minute] 0 to 1439 of [day]. */
data class LocalMoment(val day: CalendarDay, val minute: Int)

/**
 * Whether a ferry is open at a moment and, if that can be told, when that changes next.
 *
 * @property next `null` when nothing changes within a year, or when the next change is not certain
 */
data class OpeningStatus(val open: Boolean, val next: LocalMoment?)

/**
 * OpenStreetMap's `opening_hours` as the ferries of the networks use it, see `show-ferry-open-now`:
 * date and weekday selectors, public and school holidays, time spans, `off` and `unknown`. Answers
 * only what the value tells for sure.
 */
class OpeningHours private constructor(private val readings: List<List<Rule>>) {

  private val usesPublicHolidays = readings.flatten().any { it.days?.uses(Holiday.PUBLIC) == true }
  private val usesSchoolHolidays = readings.flatten().any { it.days?.uses(Holiday.SCHOOL) == true }

  /**
   * The status at [moment], or `null` when the value does not tell, e.g. because it depends on
   * school holidays.
   */
  fun statusAt(moment: LocalMoment): OpeningStatus? {
    val timeline = Timeline()
    val current = timeline.stateAt(moment) ?: return null
    for ((day, start, state) in timeline.changesAfter(moment)) {
      if (state == current) continue
      return OpeningStatus(current == State.OPEN, state?.let { LocalMoment(day, start) })
    }
    return OpeningStatus(current == State.OPEN, null)
  }

  /** What the day's plans tell for sure, per day, built while walking forward. */
  private inner class Timeline {

    private val plans = mutableMapOf<CalendarDay, List<Plan>>()

    private fun plansOf(day: CalendarDay) = plans.getOrPut(day) { possiblePlans(day) }

    /**
     * The certain state from each start of a day, `null` where the possible plans disagree or
     * cannot tell.
     */
    fun certain(day: CalendarDay): List<Pair<Int, State?>> {
      val today = plansOf(day)
      val yesterday = plansOf(day.plusDays(-1))
      val starts = mutableSetOf(0)
      for (segment in today.flatMap { it.segments }) {
        starts += listOf(segment.start, segment.end).filter { it < MINUTES_PER_DAY }
      }
      // What yesterday has past midnight.
      for (segment in yesterday.flatMap { it.segments }) {
        starts +=
          listOf(segment.start, segment.end)
            .filter { it > MINUTES_PER_DAY && it < 2 * MINUTES_PER_DAY }
            .map { it - MINUTES_PER_DAY }
      }
      val result = mutableListOf<Pair<Int, State?>>()
      for (start in starts.sorted()) {
        val states = today.flatMap { plan -> yesterday.map { plan.stateAt(start, it) } }.toSet()
        val state = states.singleOrNull()?.takeUnless { it == State.UNKNOWN }
        if (result.isEmpty() || result.last().second != state) result += start to state
      }
      return result
    }

    fun stateAt(moment: LocalMoment): State? =
      certain(moment.day).last { it.first <= moment.minute }.second

    /** The certain states after [moment], up to a year ahead. */
    fun changesAfter(moment: LocalMoment): Sequence<Triple<CalendarDay, Int, State?>> = sequence {
      certain(moment.day)
        .filter { it.first > moment.minute }
        .forEach { yield(Triple(moment.day, it.first, it.second)) }
      for (offset in 1..DAYS_AHEAD) {
        val day = moment.day.plusDays(offset)
        certain(day).forEach { yield(Triple(day, it.first, it.second)) }
      }
    }
  }

  /**
   * The plans of [day] for every reading of the value and every answer the holidays it depends on
   * may have.
   */
  private fun possiblePlans(day: CalendarDay): List<Plan> {
    val publicHoliday =
      if (usesPublicHolidays) PublicHolidays.isHoliday(day)?.let(::listOf) ?: listOf(true, false)
      else listOf(false)
    val schoolHoliday = if (usesSchoolHolidays) listOf(true, false) else listOf(false)
    return readings.flatMap { rules ->
      publicHoliday.flatMap { public -> schoolHoliday.map { plan(rules, day, public, it) } }
    }
  }

  /** The day's open and unknown times, on minutes 0 to 2879, including past midnight. */
  private fun plan(
    rules: List<Rule>,
    day: CalendarDay,
    publicHoliday: Boolean,
    schoolHoliday: Boolean,
  ): Plan {
    var segments = listOf<Segment>()
    for (rule in rules) {
      if (rule.note) continue
      if (rule.dates.isNotEmpty() && rule.dates.none { it.contains(day) }) continue
      if (rule.days != null && !rule.days.matches(day, publicHoliday, schoolHoliday)) continue
      val state = rule.state
      if (!rule.additional) segments = listOf()
      for (span in rule.spans) segments = paint(segments, span.first, span.last + 1, state)
    }
    return Plan(segments)
  }

  private enum class State {
    OPEN,
    CLOSED,
    UNKNOWN,
  }

  /** [start] inclusive, [end] exclusive, in minutes of the day the plan is for. */
  private data class Segment(val start: Int, val end: Int, val state: State)

  private class Plan(val segments: List<Segment>) {

    private fun stateAt(minute: Int) =
      segments.firstOrNull { minute >= it.start && minute < it.end }?.state ?: State.CLOSED

    /** The state at [minute] of this day, including what [yesterday] has past midnight. */
    fun stateAt(minute: Int, yesterday: Plan): State {
      val states = setOf(stateAt(minute), yesterday.stateAt(minute + MINUTES_PER_DAY))
      return when {
        State.OPEN in states -> State.OPEN
        State.UNKNOWN in states -> State.UNKNOWN
        else -> State.CLOSED
      }
    }
  }

  private fun paint(segments: List<Segment>, start: Int, end: Int, state: State): List<Segment> {
    val result = mutableListOf<Segment>()
    for (segment in segments) {
      if (segment.end <= start || segment.start >= end) {
        result += segment
        continue
      }
      if (segment.start < start) result += segment.copy(end = start)
      if (segment.end > end) result += segment.copy(start = end)
    }
    if (state != State.CLOSED) result += Segment(start, end, state)
    return result.sortedBy { it.start }
  }

  /** One rule; [spans] are minutes from the day's start, up to 2880 for spans past midnight. */
  private data class Rule(
    val additional: Boolean,
    val dates: List<DateRange>,
    val days: DaySelector?,
    val spans: List<IntRange>,
    val state: State,
    /** Only a comment, e.g. a trailing note about the weather: not part of the times. */
    val note: Boolean,
  )

  private enum class Holiday {
    PUBLIC,
    SCHOOL,
  }

  private sealed interface DayItem {
    fun matches(day: CalendarDay, publicHoliday: Boolean, schoolHoliday: Boolean): Boolean

    data class Weekdays(val from: Int, val to: Int) : DayItem {
      override fun matches(day: CalendarDay, publicHoliday: Boolean, schoolHoliday: Boolean) =
        if (from <= to) day.dayOfWeek in from..to else day.dayOfWeek >= from || day.dayOfWeek <= to
    }

    data class NthWeekday(val weekday: Int, val nth: Int) : DayItem {
      override fun matches(day: CalendarDay, publicHoliday: Boolean, schoolHoliday: Boolean) =
        CalendarDay.nthDayOfWeek(day.year, day.month, weekday, nth) == day
    }

    data class Of(val holiday: Holiday) : DayItem {
      override fun matches(day: CalendarDay, publicHoliday: Boolean, schoolHoliday: Boolean) =
        if (holiday == Holiday.PUBLIC) publicHoliday else schoolHoliday
    }
  }

  /** Items of each group are alternatives; all groups must match, e.g. `SH Mo-Fr`. */
  private class DaySelector(val groups: List<List<DayItem>>) {
    fun matches(day: CalendarDay, publicHoliday: Boolean, schoolHoliday: Boolean) =
      groups.all { group ->
        group.any { it.matches(day, publicHoliday, schoolHoliday) }
      }

    fun uses(holiday: Holiday) = groups.flatten().contains(DayItem.Of(holiday))
  }

  /** A point of the calendar, resolved for a year: a month, a date, an nth weekday or Easter. */
  private sealed interface DatePoint {
    val year: Int?

    /** The first day the point stands for in [year]. */
    fun first(year: Int): CalendarDay

    /** The last day, e.g. the month's last day for a month. */
    fun last(year: Int): CalendarDay = first(year)

    data class Month(override val year: Int?, val month: Int) : DatePoint {
      override fun first(year: Int) = CalendarDay(year, month, 1)

      override fun last(year: Int) =
        CalendarDay(year, month, CalendarDay.lengthOfMonth(year, month))
    }

    data class Date(override val year: Int?, val month: Int, val day: Int) : DatePoint {
      override fun first(year: Int) =
        CalendarDay(year, month, minOf(day, CalendarDay.lengthOfMonth(year, month)))
    }

    data class NthWeekday(override val year: Int?, val month: Int, val weekday: Int, val nth: Int) :
      DatePoint {
      override fun first(year: Int) = CalendarDay.nthDayOfWeek(year, month, weekday, nth)
    }

    data class Easter(override val year: Int?, val offset: Int) : DatePoint {
      override fun first(year: Int) = CalendarDay.easter(year).plusDays(offset)
    }
  }

  /** From [from] to [to], both included, across the year's end if [to] comes before [from]. */
  private class DateRange(val from: DatePoint, val to: DatePoint?) {
    fun contains(day: CalendarDay): Boolean {
      val years = from.year?.let(::listOf) ?: listOf(day.year - 1, day.year)
      return years.any { year ->
        val first = from.first(year)
        val last =
          if (to == null) from.last(year)
          else
            to.last(to.year ?: year).let {
              if (to.year == null && it < first) to.last(year + 1) else it
            }
        day in first..last
      }
    }
  }

  companion object {

    private const val MINUTES_PER_DAY = 24 * 60

    /** How far the next change is looked for. */
    private const val DAYS_AHEAD = 366

    private val months =
      listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val weekdays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    private val variableTimes = setOf("sunrise", "sunset", "dawn", "dusk")

    /** The rules of [text], or `null` if it uses syntax this reader does not know. */
    fun parse(text: String): OpeningHours? {
      val tokens = tokenize(text) ?: return null
      val rules = runCatching { Parser(tokens).rules() }.getOrNull() ?: return null
      if (rules.all { it.note }) return null
      val inheriting = inheritingDates(rules)
      return OpeningHours(if (inheriting == rules) listOf(rules) else listOf(rules, inheriting))
    }

    /**
     * The other reading of additional rules without dates after a rule with dates, e.g. `Sa` in
     * `Apr-Sep: Mo-Fr 06:00-21:00, Sa 08:00-21:00`: the specification applies it all year, but the
     * author often means it only for the dates before it.
     */
    private fun inheritingDates(rules: List<Rule>): List<Rule> {
      var dates = listOf<DateRange>()
      return rules.map { rule ->
        when {
          rule.note -> rule
          !rule.additional || rule.dates.isNotEmpty() -> rule.also { dates = it.dates }
          else -> rule.copy(dates = dates)
        }
      }
    }

    private sealed interface Token {
      data class Number(val value: Int, val digits: Int) : Token

      data class Word(val text: String) : Token

      data class Time(val minutes: Int) : Token

      data class Comment(val text: String) : Token

      data class Symbol(val char: Char) : Token
    }

    private fun tokenize(text: String): List<Token>? {
      val tokens = mutableListOf<Token>()
      var index = 0
      while (index < text.length) {
        val char = text[index]
        when {
          char.isWhitespace() -> index++
          char == '"' -> {
            val end = text.indexOf('"', index + 1)
            if (end < 0) return null
            tokens += Token.Comment(text.substring(index + 1, end))
            index = end + 1
          }
          char.isDigit() -> {
            val end = text.indexOfFirstFrom(index) { !it.isDigit() }
            val digits = text.substring(index, end)
            if (
              end + 2 < text.length &&
                text[end] == ':' &&
                text[end + 1].isDigit() &&
                text[end + 2].isDigit() &&
                digits.length <= 2
            ) {
              val minutes = digits.toInt() * 60 + text.substring(end + 1, end + 3).toInt()
              tokens += Token.Time(minutes)
              index = end + 3
            } else {
              tokens += Token.Number(digits.toInt(), digits.length)
              index = end
            }
          }
          char.isLetter() -> {
            val end = text.indexOfFirstFrom(index) { !it.isLetter() }
            tokens += Token.Word(text.substring(index, end))
            index = end
          }
          char in "-,;:[]+()/" -> {
            tokens += Token.Symbol(char)
            index++
          }
          else -> return null
        }
      }
      return tokens
    }

    private fun String.indexOfFirstFrom(start: Int, predicate: (Char) -> Boolean): Int {
      for (index in start until length) if (predicate(this[index])) return index
      return length
    }

    private class Parser(private val tokens: List<Token>) {
      private var position = 0

      private fun peek(offset: Int = 0) = tokens.getOrNull(position + offset)

      private fun next() = tokens[position++]

      private fun isSymbol(char: Char, offset: Int = 0) = peek(offset) == Token.Symbol(char)

      private fun expect(char: Char) {
        require(isSymbol(char)) { "expected $char" }
        position++
      }

      private fun word(offset: Int = 0) = (peek(offset) as? Token.Word)?.text

      private fun isMonth(offset: Int = 0) = word(offset) in months

      private fun isWeekday(offset: Int = 0) = word(offset) in weekdays

      private fun isHoliday(offset: Int = 0) = word(offset) == "PH" || word(offset) == "SH"

      fun rules(): List<Rule> {
        val rules = mutableListOf<Rule>()
        var additional = false
        while (peek() != null) {
          rules += rule(additional)
          when {
            peek() == null -> break
            isSymbol(';') -> additional = false
            isSymbol(',') -> additional = true
            else -> error("unexpected ${peek()}")
          }
          position++
        }
        return rules
      }

      private fun rule(additional: Boolean): Rule {
        val dates = dates()
        if (dates.isNotEmpty() && isSymbol(':')) position++
        val days = days()
        if (days != null && isSymbol(':')) position++
        val (spans, variable) = spans()
        val modifier = word()?.takeIf { it in setOf("open", "off", "closed", "unknown") }
        if (modifier != null) position++
        val comment = (peek() as? Token.Comment)?.also { position++ }
        val selected = dates.isNotEmpty() || days != null
        require(selected || spans.isNotEmpty() || variable || modifier != null || comment != null) {
          "empty rule"
        }
        val state =
          when {
            // A span from sunset to sunrise is unknown, whether it is open or closed.
            modifier == "unknown" || variable -> State.UNKNOWN
            modifier == "off" || modifier == "closed" -> State.CLOSED
            modifier == null && spans.isEmpty() && comment != null -> State.UNKNOWN
            else -> State.OPEN
          }
        val note = !selected && spans.isEmpty() && modifier == null && comment != null
        return Rule(
          additional,
          dates,
          days,
          spans.ifEmpty { listOf(0 until MINUTES_PER_DAY) },
          state,
          note,
        )
      }

      private fun dates(): List<DateRange> {
        val dates = mutableListOf<DateRange>()
        if (!startsDate()) return dates
        var month: Int? = null
        while (true) {
          val range =
            if (peek() is Token.Number && (peek() as Token.Number).digits <= 2 && month != null) {
              dayRange(null, month)
            } else {
              dateRange()
            }
          dates += range
          month = monthOf(range.to ?: range.from)
          if (isSymbol(',') && (startsDate(1) || peek(1) is Token.Number)) position++ else break
        }
        return dates
      }

      private fun monthOf(point: DatePoint) =
        when (point) {
          is DatePoint.Month -> point.month
          is DatePoint.Date -> point.month
          is DatePoint.NthWeekday -> point.month
          is DatePoint.Easter -> null
        }

      private fun startsDate(offset: Int = 0) =
        isMonth(offset) || word(offset) == "easter" || (peek(offset) as? Token.Number)?.digits == 4

      /** A day of [month], or a range of days of it, e.g. `14` or `20-22` after `Apr 13,`. */
      private fun dayRange(year: Int?, month: Int): DateRange {
        val from = DatePoint.Date(year, month, (next() as Token.Number).value)
        if (isSymbol('-') && (peek(1) as? Token.Number)?.let { it.digits <= 2 } == true) {
          position++
          return DateRange(from, DatePoint.Date(year, month, (next() as Token.Number).value))
        }
        return DateRange(from, null)
      }

      private fun dateRange(): DateRange {
        val from = datePoint()
        if (!isSymbol('-')) return DateRange(from, null)
        val day = peek(1) as? Token.Number
        if (day != null && day.digits <= 2 && from is DatePoint.Date) {
          position++
          return DateRange(
            from,
            DatePoint.Date(from.year, from.month, (next() as Token.Number).value),
          )
        }
        if (!startsDate(1)) return DateRange(from, null)
        position++
        return DateRange(from, datePoint())
      }

      private fun datePoint(): DatePoint {
        val year = (peek() as? Token.Number)?.takeIf { it.digits == 4 }?.also { position++ }?.value
        if (word() == "easter") {
          position++
          return DatePoint.Easter(year, offset())
        }
        val month = months.indexOf(word()) + 1
        require(month > 0) { "expected a month" }
        position++
        val day = peek() as? Token.Number
        if (day != null && day.digits <= 2) {
          position++
          return DatePoint.Date(year, month, day.value)
        }
        if (isWeekday() && isSymbol('[', 1)) {
          val weekday = weekdays.indexOf(word()) + 1
          position++
          return DatePoint.NthWeekday(year, month, weekday, nth())
        }
        return DatePoint.Month(year, month)
      }

      /** e.g. ` +40 days` or ` -2 days` after `easter`, or 0. */
      private fun offset(): Int {
        val sign =
          when {
            isSymbol('+') -> 1
            isSymbol('-') -> -1
            else -> return 0
          }
        val days = peek(1) as? Token.Number ?: return 0
        if (word(2) != "day" && word(2) != "days") return 0
        position += 3
        return sign * days.value
      }

      /** e.g. `[2]` or `[-1]`. */
      private fun nth(): Int {
        expect('[')
        val sign = if (isSymbol('-')) -1.also { position++ } else 1
        val nth = (next() as Token.Number).value
        expect(']')
        return sign * nth
      }

      private fun days(): DaySelector? {
        val groups = mutableListOf<List<DayItem>>()
        while (isWeekday() || isHoliday()) {
          val group = mutableListOf<DayItem>()
          while (true) {
            group += dayItem()
            if (isSymbol(',') && (isWeekday(1) || isHoliday(1))) position++ else break
          }
          groups += group
        }
        return if (groups.isEmpty()) null else DaySelector(groups)
      }

      private fun dayItem(): DayItem {
        val text = (next() as Token.Word).text
        if (text == "PH") return DayItem.Of(Holiday.PUBLIC)
        if (text == "SH") return DayItem.Of(Holiday.SCHOOL)
        val weekday = weekdays.indexOf(text) + 1
        if (isSymbol('[')) return DayItem.NthWeekday(weekday, nth())
        if (isSymbol('-') && isWeekday(1)) {
          position++
          return DayItem.Weekdays(weekday, weekdays.indexOf((next() as Token.Word).text) + 1)
        }
        return DayItem.Weekdays(weekday, weekday)
      }

      /** The time spans, and whether one of them depends on sunrise or sunset. */
      private fun spans(): Pair<List<IntRange>, Boolean> {
        if (
          (peek() as? Token.Number)?.value == 24 &&
            isSymbol('/', 1) &&
            (peek(2) as? Token.Number)?.value == 7
        ) {
          position += 3
          return listOf(0 until MINUTES_PER_DAY) to false
        }
        val spans = mutableListOf<IntRange>()
        var variable = false
        while (startsTime()) {
          val start = time()
          expect('-')
          val end = time()
          if (start == null || end == null) variable = true
          else spans += start until (if (end <= start) end + MINUTES_PER_DAY else end)
          if (isSymbol(',') && startsTime(1)) position++ else break
        }
        return spans to variable
      }

      private fun startsTime(offset: Int = 0) =
        peek(offset) is Token.Time || isSymbol('(', offset) || word(offset) in variableTimes

      /** Minutes of the day, or `null` for a time that depends on the sun. */
      private fun time(): Int? {
        when (val token = next()) {
          is Token.Time -> return token.minutes
          Token.Symbol('(') -> {
            require(word() in variableTimes) { "expected sunrise or sunset" }
            position++
            require(isSymbol('+') || isSymbol('-')) { "expected an offset" }
            position++
            require(next() is Token.Time) { "expected a time" }
            expect(')')
            return null
          }
          else -> {
            require((token as? Token.Word)?.text in variableTimes) { "expected a time" }
            return null
          }
        }
      }
    }
  }
}
