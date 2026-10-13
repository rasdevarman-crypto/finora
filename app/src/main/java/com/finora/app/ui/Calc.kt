package com.finora.app.ui

import com.finora.app.data.Bazar
import com.finora.app.data.Deposit
import com.finora.app.data.Meal
import com.finora.app.data.Member
import com.finora.app.data.SharedCost
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

data class Ledger(
    val members: List<Member>,
    val deposits: List<Deposit>,
    val bazars: List<Bazar>,
    val shared: List<SharedCost>,
    val meals: List<Meal>
)

data class MemberMonth(val deposit: Double, val meals: Double, val mealCost: Double, val shared: Double) {
    val net: Double get() = deposit - mealCost - shared
}

data class MonthTotals(
    val deposit: Double,
    val bazar: Double,
    val meals: Double,
    val rate: Double,
    val shared: Double
)

/** balance > 0 : member gets money back. balance < 0 : member owes money. */
data class MemberRow(val member: Member, val month: MemberMonth, val carry: Double) {
    val balance: Double get() = carry + month.net
}

data class Summary(val totals: MonthTotals, val rows: List<MemberRow>)

object Calc {
    fun monthOf(epochDay: Long): YearMonth = YearMonth.from(LocalDate.ofEpochDay(epochDay))

    fun isIn(m: Member, ym: YearMonth): Boolean {
        val start = YearMonth.parse(m.startMonth)
        val end = m.endMonth?.let { YearMonth.parse(it) }
        return !ym.isBefore(start) && (end == null || !ym.isAfter(end))
    }

    private fun calcMonth(l: Ledger, ym: YearMonth): Pair<MonthTotals, Map<Long, MemberMonth>> {
        val dep = l.deposits.filter { monthOf(it.date) == ym }
        val bazarTotal = l.bazars.filter { monthOf(it.date) == ym }.sumOf { it.amount }
        val sharedTotal = l.shared.filter { monthOf(it.date) == ym }.sumOf { it.amount }
        val meals = l.meals.filter { monthOf(it.date) == ym }
        val totalMeals = meals.sumOf { it.count }
        val rate = if (totalMeals > 0) bazarTotal / totalMeals else 0.0

        val inMembers = l.members.filter { isIn(it, ym) }
        val perHead = if (inMembers.isNotEmpty()) sharedTotal / inMembers.size else 0.0
        val inIds = inMembers.map { it.id }.toSet()

        val perMember = l.members.associate { m ->
            val d = dep.filter { it.memberId == m.id }.sumOf { it.amount }
            val ml = meals.filter { it.memberId == m.id }.sumOf { it.count }
            m.id to MemberMonth(d, ml, ml * rate, if (m.id in inIds) perHead else 0.0)
        }
        return MonthTotals(dep.sumOf { it.amount }, bazarTotal, totalMeals, rate, sharedTotal) to perMember
    }

    fun summary(l: Ledger, selected: YearMonth): Summary {
        val earlier = (
            l.deposits.map { monthOf(it.date) } +
                l.bazars.map { monthOf(it.date) } +
                l.shared.map { monthOf(it.date) } +
                l.meals.map { monthOf(it.date) }
            ).filter { it.isBefore(selected) }.toSet()

        val carry = HashMap<Long, Double>()
        for (m in earlier) {
            calcMonth(l, m).second.forEach { (id, mm) -> carry[id] = (carry[id] ?: 0.0) + mm.net }
        }

        val (totals, perMember) = calcMonth(l, selected)
        val rows = l.members.mapNotNull { m ->
            val mm = perMember.getValue(m.id)
            val c = carry[m.id] ?: 0.0
            if (isIn(m, selected) || abs(c + mm.net) >= 0.01) MemberRow(m, mm, c) else null
        }
        return Summary(totals, rows)
    }
}
