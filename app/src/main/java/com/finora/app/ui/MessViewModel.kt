package com.finora.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finora.app.data.AppDb
import com.finora.app.data.Bazar
import com.finora.app.data.Deposit
import com.finora.app.data.Meal
import com.finora.app.data.Member
import com.finora.app.data.SharedCost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class UiState(
    val month: YearMonth,
    val ledger: Ledger,
    val summary: Summary,
    val activeMembers: List<Member>,
    val deposits: List<Deposit>,
    val bazars: List<Bazar>,
    val shared: List<SharedCost>,
    val meals: List<Meal>
)

class MessViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    private val selected = MutableStateFlow(YearMonth.now())

    private val ledger = combine(
        dao.members(), dao.deposits(), dao.bazars(), dao.sharedCosts(), dao.meals()
    ) { m, d, b, s, ml -> Ledger(m, d, b, s, ml) }

    val state: StateFlow<UiState?> = combine(selected, ledger) { month, l ->
        UiState(
            month = month,
            ledger = l,
            summary = Calc.summary(l, month),
            activeMembers = l.members.filter { Calc.isIn(it, month) },
            deposits = l.deposits.filter { Calc.monthOf(it.date) == month },
            bazars = l.bazars.filter { Calc.monthOf(it.date) == month },
            shared = l.shared.filter { Calc.monthOf(it.date) == month },
            meals = l.meals.filter { Calc.monthOf(it.date) == month }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun shiftMonth(delta: Long) {
        selected.value = selected.value.plusMonths(delta)
    }

    private fun dateOf(day: Int): Long {
        val m = selected.value
        return m.atDay(day.coerceIn(1, m.lengthOfMonth())).toEpochDay()
    }

    fun addMember(name: String) = viewModelScope.launch {
        dao.insertMember(Member(name = name.trim(), startMonth = selected.value.toString()))
    }

    /** Member stays in the mess up to and including the selected month. */
    fun archiveMember(m: Member) = viewModelScope.launch {
        dao.updateMember(m.copy(endMonth = selected.value.toString()))
    }

    fun addDeposit(memberId: Long, amount: Double, day: Int, note: String) = viewModelScope.launch {
        dao.insertDeposit(Deposit(memberId = memberId, amount = amount, date = dateOf(day), note = note.trim()))
    }

    fun deleteDeposit(d: Deposit) = viewModelScope.launch { dao.deleteDeposit(d) }

    fun addBazar(amount: Double, day: Int, note: String) = viewModelScope.launch {
        dao.insertBazar(Bazar(amount = amount, date = dateOf(day), note = note.trim()))
    }

    fun deleteBazar(b: Bazar) = viewModelScope.launch { dao.deleteBazar(b) }

    fun addShared(title: String, amount: Double) = viewModelScope.launch {
        dao.insertShared(SharedCost(title = title.trim(), amount = amount, date = dateOf(1)))
    }

    fun deleteShared(s: SharedCost) = viewModelScope.launch { dao.deleteShared(s) }

    fun setMeal(memberId: Long, epochDay: Long, count: Double) = viewModelScope.launch {
        dao.upsertMeal(Meal(memberId, epochDay, count))
    }
}
