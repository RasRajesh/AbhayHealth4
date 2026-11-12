package com.example.abhayhealth.feature.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abhayhealth.core.common.DefaultDispatcherProvider
import com.example.abhayhealth.core.common.DispatcherProvider
import com.example.abhayhealth.domain.model.AuthState
import com.example.abhayhealth.domain.repository.AuthRepository
import com.example.abhayhealth.feature.records.ExtractedMedicalInfo
import com.example.abhayhealth.feature.records.MedicalFieldParser
import com.example.abhayhealth.feature.records.MedicalRecordRepository
import com.example.abhayhealth.feature.symptoms.SymptomLogEntity
import com.example.abhayhealth.feature.symptoms.SymptomLogRepository
import com.example.abhayhealth.feature.vitals.VitalReadingEntity
import com.example.abhayhealth.feature.vitals.VitalReadingRepository
import com.example.abhayhealth.feature.vitals.VitalType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BiomarkerPoint(
    val recordDate: String,
    val timestamp: Long,
    val value: String,
    val numericValue: Double?,
    val unit: String
)

data class BiomarkerTrend(
    val biomarkerName: String,
    val latestValue: String,
    val numericValue: Double?,
    val unit: String,
    val referenceRange: String,
    val category: String, // "LABS" or "VITALS"
    val clinicalGuidelineTag: String? = null,
    val deltaFromPrevious: String? = null,
    val points: List<BiomarkerPoint>
)

data class DualMetricPoint(
    val dateLabel: String,
    val timestamp: Long,
    val value1: Double,
    val value2: Double
)

data class DualMetricComparison(
    val metric1Name: String,
    val metric1Unit: String,
    val metric2Name: String,
    val metric2Unit: String,
    val points: List<DualMetricPoint>,
    val correlationInsight: String,
    val isSampleData: Boolean = false
)

class TrendsViewModel(
    private val medicalRecordRepository: MedicalRecordRepository? = null,
    private val authRepository: AuthRepository? = null,
    private val symptomLogRepository: SymptomLogRepository? = null,
    private val vitalReadingRepository: VitalReadingRepository? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    private val _labTrends = MutableStateFlow<List<BiomarkerTrend>>(emptyList())
    val labTrends: StateFlow<List<BiomarkerTrend>> = _labTrends.asStateFlow()

    private val _vitalsTrends = MutableStateFlow<List<BiomarkerTrend>>(emptyList())
    val vitalsTrends: StateFlow<List<BiomarkerTrend>> = _vitalsTrends.asStateFlow()

    private val _biomarkerTrends = MutableStateFlow<List<BiomarkerTrend>>(emptyList())
    val biomarkerTrends: StateFlow<List<BiomarkerTrend>> = _biomarkerTrends.asStateFlow()

    private val _comparisonMetric1 = MutableStateFlow("Weight")
    val comparisonMetric1: StateFlow<String> = _comparisonMetric1.asStateFlow()

    private val _comparisonMetric2 = MutableStateFlow("Blood Pressure (Systolic)")
    val comparisonMetric2: StateFlow<String> = _comparisonMetric2.asStateFlow()

    private val _availableComparisonMetrics = MutableStateFlow(
        listOf(
            "Weight",
            "Blood Pressure (Systolic)",
            "Blood Pressure (Diastolic)",
            "Blood Glucose",
            "Heart Rate",
            "SpO2"
        )
    )
    val availableComparisonMetrics: StateFlow<List<String>> = _availableComparisonMetrics.asStateFlow()

    private val _dualComparison = MutableStateFlow(
        createDefaultComparison("Weight", "Blood Pressure (Systolic)")
    )
    val dualComparison: StateFlow<DualMetricComparison> = _dualComparison.asStateFlow()

    private val _symptoms = MutableStateFlow<List<SymptomLogEntity>>(emptyList())
    val symptoms: StateFlow<List<SymptomLogEntity>> = _symptoms.asStateFlow()

    private val _showAddSymptomDialog = MutableStateFlow(false)
    val showAddSymptomDialog: StateFlow<Boolean> = _showAddSymptomDialog.asStateFlow()

    init {
        observeBiomarkers()
        observeSymptoms()
    }

    private fun observeBiomarkers() {
        val recordRepo = medicalRecordRepository ?: return
        viewModelScope.launch(dispatcherProvider.main) {
            val authRepo = authRepository
            if (authRepo != null) {
                authRepo.authState.collectLatest { state ->
                    val uid = when (state) {
                        is AuthState.Authenticated -> state.user.uid
                        else -> authRepo.currentUser?.uid?.ifBlank { null } ?: "local_user"
                    }
                    observeForUid(uid, recordRepo)
                }
            } else {
                observeForUid("local_user", recordRepo)
            }
        }
    }

    private suspend fun observeForUid(uid: String, recordRepo: MedicalRecordRepository) {
        val vitalRepo = vitalReadingRepository
        val extractedFlow = recordRepo.getAllExtractedInfo(uid)
        val vitalsFlow = vitalRepo?.getAllReadings(uid)

        val combinedFlow = if (vitalsFlow != null) {
            combine(extractedFlow, vitalsFlow) { infoList, vitalsList ->
                Pair(infoList, vitalsList)
            }
        } else {
            extractedFlow.combine(flowOf(emptyList<VitalReadingEntity>())) { infoList, vitalsList ->
                Pair(infoList, vitalsList)
            }
        }

        combinedFlow.collectLatest { (infoList, vitalsList) ->
            val trendsMap = mutableMapOf<String, MutableList<BiomarkerPoint>>()
            val rangeMap = mutableMapOf<String, String>()
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())

            // 1. Process Extracted Medical Document Info
            infoList.forEach { info ->
                if (info.structuredResultsJson.isNotBlank() && info.structuredResultsJson != "[]") {
                    val results = MedicalFieldParser.deserializeResults(info.structuredResultsJson)
                    val dateStr = info.reportDate ?: sdf.format(Date(info.updatedAtTimestamp))
                    val timestamp = info.reportDate?.let { MedicalFieldParser.parseDateToTimestamp(it) } ?: info.updatedAtTimestamp

                    results.forEach { result ->
                        val testName = result.testName.trim()
                        val valStr = result.value.trim()

                        if (testName.isNotBlank() && valStr.isNotBlank()) {
                            val numValue = extractNumericValue(valStr)
                            val list = trendsMap.getOrPut(testName) { mutableListOf() }
                            list.add(
                                BiomarkerPoint(
                                    recordDate = dateStr,
                                    timestamp = timestamp,
                                    value = valStr,
                                    numericValue = numValue,
                                    unit = result.unit.trim()
                                )
                            )
                            if (result.referenceRange.isNotBlank()) {
                                rangeMap[testName] = result.referenceRange
                            }
                        }
                    }
                }
            }

            // 2. Process Vitals
            vitalsList.forEach { vital ->
                val dateStr = sdf.format(Date(vital.timestamp))
                if (vital.type == VitalType.BLOOD_PRESSURE.name) {
                    if (vital.systolic != null) {
                        val sList = trendsMap.getOrPut("Blood Pressure (Systolic)") { mutableListOf() }
                        sList.add(
                            BiomarkerPoint(
                                recordDate = dateStr,
                                timestamp = vital.timestamp,
                                value = "${vital.systolic.toInt()}",
                                numericValue = vital.systolic,
                                unit = "mmHg"
                            )
                        )
                        rangeMap["Blood Pressure (Systolic)"] = "< 120 mmHg"
                    }
                    if (vital.diastolic != null) {
                        val dList = trendsMap.getOrPut("Blood Pressure (Diastolic)") { mutableListOf() }
                        dList.add(
                            BiomarkerPoint(
                                recordDate = dateStr,
                                timestamp = vital.timestamp,
                                value = "${vital.diastolic.toInt()}",
                                numericValue = vital.diastolic,
                                unit = "mmHg"
                            )
                        )
                        rangeMap["Blood Pressure (Diastolic)"] = "< 80 mmHg"
                    }
                } else if (vital.type == VitalType.WEIGHT.name && vital.weightKg != null) {
                    val wList = trendsMap.getOrPut("Weight") { mutableListOf() }
                    wList.add(
                        BiomarkerPoint(
                            recordDate = dateStr,
                            timestamp = vital.timestamp,
                            value = "${vital.weightKg}",
                            numericValue = vital.weightKg,
                            unit = "kg"
                        )
                    )
                    rangeMap["Weight"] = "Healthy BMI Range"
                }
            }

            val allTrends = trendsMap.map { (name, points) ->
                // Sort chronologically for time-series visualization
                val sortedPoints = points.sortedBy { it.timestamp }
                val latest = sortedPoints.lastOrNull()
                val category = categorizeBiomarker(name)
                val latestNum = latest?.numericValue
                val guidelineTag = evaluateClinicalGuideline(name, latestNum)

                val previous = if (sortedPoints.size >= 2) sortedPoints[sortedPoints.size - 2] else null
                val deltaStr = if (latestNum != null && previous?.numericValue != null) {
                    val diff = latestNum - previous.numericValue
                    val formattedDiff = String.format(Locale.US, "%.1f", kotlin.math.abs(diff))
                    when {
                        diff > 0.05 -> "↑ +$formattedDiff ${latest.unit}".trim()
                        diff < -0.05 -> "↓ -$formattedDiff ${latest.unit}".trim()
                        else -> "Stable"
                    }
                } else null

                BiomarkerTrend(
                    biomarkerName = name,
                    latestValue = latest?.value ?: "--",
                    numericValue = latestNum,
                    unit = latest?.unit ?: "",
                    referenceRange = rangeMap[name] ?: "",
                    category = category,
                    clinicalGuidelineTag = guidelineTag,
                    deltaFromPrevious = deltaStr,
                    points = sortedPoints
                )
            }.sortedByDescending { it.points.size }

            _biomarkerTrends.value = allTrends
            _labTrends.value = allTrends.filter { it.category == "LABS" }
            _vitalsTrends.value = allTrends.filter { it.category == "VITALS" }

            // Extract dynamic metrics to populate available comparison list
            val dynamicMetrics = mutableListOf("Weight", "Blood Pressure (Systolic)", "Blood Pressure (Diastolic)")
            allTrends.forEach { t ->
                if (!dynamicMetrics.contains(t.biomarkerName)) {
                    dynamicMetrics.add(t.biomarkerName)
                }
            }
            _availableComparisonMetrics.value = dynamicMetrics
            updateDualComparison()
        }
    }

    fun setComparisonMetric1(metric: String) {
        _comparisonMetric1.value = metric
        updateDualComparison()
    }

    fun setComparisonMetric2(metric: String) {
        _comparisonMetric2.value = metric
        updateDualComparison()
    }

    private fun updateDualComparison() {
        val m1Name = _comparisonMetric1.value
        val m2Name = _comparisonMetric2.value
        val allTrends = _biomarkerTrends.value

        val t1 = allTrends.firstOrNull { it.biomarkerName.equals(m1Name, ignoreCase = true) }
        val t2 = allTrends.firstOrNull { it.biomarkerName.equals(m2Name, ignoreCase = true) }

        val p1 = t1?.points?.filter { it.numericValue != null } ?: emptyList()
        val p2 = t2?.points?.filter { it.numericValue != null } ?: emptyList()

        if (p1.size >= 2 && p2.size >= 2) {
            val mergedPoints = mutableListOf<DualMetricPoint>()
            p1.forEach { pt1 ->
                val matchingPt2 = p2.minByOrNull { kotlin.math.abs(it.timestamp - pt1.timestamp) }
                if (matchingPt2 != null && pt1.numericValue != null && matchingPt2.numericValue != null) {
                    mergedPoints.add(
                        DualMetricPoint(
                            dateLabel = pt1.recordDate,
                            timestamp = pt1.timestamp,
                            value1 = pt1.numericValue,
                            value2 = matchingPt2.numericValue
                        )
                    )
                }
            }
            if (mergedPoints.size >= 2) {
                val insight = generateCorrelationInsight(m1Name, m2Name, mergedPoints)
                _dualComparison.value = DualMetricComparison(
                    metric1Name = m1Name,
                    metric1Unit = t1?.unit ?: "",
                    metric2Name = m2Name,
                    metric2Unit = t2?.unit ?: "",
                    points = mergedPoints.sortedBy { it.timestamp },
                    correlationInsight = insight,
                    isSampleData = false
                )
                return
            }
        }

        _dualComparison.value = createDefaultComparison(m1Name, m2Name)
    }

    private fun createDefaultComparison(m1: String, m2: String): DualMetricComparison {
        val (unit1, unit2) = when {
            m1.contains("Weight", true) && m2.contains("Pressure", true) -> Pair("kg", "mmHg")
            m1.contains("Pressure", true) && m2.contains("Weight", true) -> Pair("mmHg", "kg")
            m1.contains("Glucose", true) -> Pair("mg/dL", "mmHg")
            m2.contains("Glucose", true) -> Pair("kg", "mg/dL")
            m1.contains("Heart", true) || m1.contains("Pulse", true) -> Pair("BPM", "mmHg")
            else -> Pair("unit", "unit")
        }

        val samplePoints = listOf(
            DualMetricPoint("01 Oct", 1000L, 72.4, 128.0),
            DualMetricPoint("08 Oct", 2000L, 71.8, 126.0),
            DualMetricPoint("15 Oct", 3000L, 71.2, 123.0),
            DualMetricPoint("22 Oct", 4000L, 70.6, 121.0),
            DualMetricPoint("29 Oct", 5000L, 70.0, 118.0)
        )

        val insight = if ((m1.contains("Weight", true) && m2.contains("Pressure", true)) ||
            (m2.contains("Weight", true) && m1.contains("Pressure", true))) {
            "Clinical Evidence: High physiological correlation. A 2.4 kg reduction in body weight corresponds to a 10 mmHg reduction in systolic blood pressure. Maintaining target weight significantly reduces hypertension risk."
        } else {
            "Comparative Analytics: Tracking $m1 alongside $m2 on the same timeline reveals longitudinal trends and concurrent metric shifts."
        }

        return DualMetricComparison(
            metric1Name = m1,
            metric1Unit = unit1,
            metric2Name = m2,
            metric2Unit = unit2,
            points = samplePoints,
            correlationInsight = insight,
            isSampleData = true
        )
    }

    private fun generateCorrelationInsight(m1: String, m2: String, points: List<DualMetricPoint>): String {
        val first = points.first()
        val last = points.last()
        val delta1 = last.value1 - first.value1
        val delta2 = last.value2 - first.value2

        val dir1 = if (delta1 > 0) "increased" else if (delta1 < 0) "decreased" else "remained steady"
        val dir2 = if (delta2 > 0) "increased" else if (delta2 < 0) "decreased" else "remained steady"

        return if ((delta1 > 0 && delta2 > 0) || (delta1 < 0 && delta2 < 0)) {
            "Positive Correlation: Over this period, $m1 $dir1 while $m2 concurrently $dir2. Monitoring both metrics concurrently assists in comprehensive lifestyle and treatment efficacy assessment."
        } else {
            "Inverse or Independent Pattern: $m1 $dir1 while $m2 $dir2. Continue logging to observe long-term clinical stabilization."
        }
    }

    private fun observeSymptoms() {
        val symptomRepo = symptomLogRepository ?: return
        viewModelScope.launch(dispatcherProvider.main) {
            val authRepo = authRepository
            if (authRepo != null) {
                authRepo.authState.collectLatest { state ->
                    val uid = when (state) {
                        is AuthState.Authenticated -> state.user.uid
                        else -> authRepo.currentUser?.uid?.ifBlank { null } ?: "local_user"
                    }
                    symptomRepo.getAllSymptoms(uid).collectLatest { list ->
                        _symptoms.value = list
                    }
                }
            } else {
                symptomRepo.getAllSymptoms("local_user").collectLatest { list ->
                    _symptoms.value = list
                }
            }
        }
    }

    fun openAddSymptomDialog() {
        _showAddSymptomDialog.value = true
    }

    fun dismissAddSymptomDialog() {
        _showAddSymptomDialog.value = false
    }

    fun logSymptom(
        symptomName: String,
        severity: String,
        severityScore: Int,
        notes: String,
        triggers: String,
        durationMinutes: Int
    ) {
        val uid = authRepository?.currentUser?.uid?.ifBlank { null } ?: "local_user"
        val repo = symptomLogRepository ?: return

        viewModelScope.launch(dispatcherProvider.main) {
            val entity = SymptomLogEntity(
                accountUid = uid,
                symptomName = symptomName,
                severity = severity,
                severityScore = severityScore,
                timestamp = System.currentTimeMillis(),
                notes = notes,
                triggers = triggers,
                durationMinutes = durationMinutes
            )
            repo.logSymptom(entity)
            _showAddSymptomDialog.value = false
        }
    }

    fun deleteSymptom(symptom: SymptomLogEntity) {
        val repo = symptomLogRepository ?: return
        viewModelScope.launch(dispatcherProvider.main) {
            repo.deleteSymptom(symptom)
        }
    }

    private fun extractNumericValue(valStr: String): Double? {
        val cleaned = valStr.replace("<", "").replace(">", "").replace("≤", "").replace("≥", "").trim()
        val match = Regex("""(\d+(?:\.\d+)?)""").find(cleaned)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun categorizeBiomarker(name: String): String {
        val lower = name.lowercase()
        return if (lower.contains("bp") || lower.contains("blood pressure") ||
            lower.contains("temp") || lower.contains("systolic") ||
            lower.contains("diastolic") || lower.contains("pulse") ||
            lower.contains("heart rate") || lower.contains("weight") ||
            lower.contains("spo2") || lower.contains("oxygen")
        ) {
            "VITALS"
        } else {
            "LABS"
        }
    }


    private fun evaluateClinicalGuideline(name: String, value: Double?): String? {
        if (value == null) return null
        val lower = name.lowercase()

        return when {
            lower.contains("glucose") || lower.contains("fbs") -> when {
                value < 100.0 -> "Normal FBS"
                value in 100.0..125.0 -> "Impaired Fasting"
                value >= 126.0 -> "Elevated FBS"
                else -> null
            }
            lower.contains("hba1c") -> when {
                value < 5.7 -> "Normal (<5.7%)"
                value in 5.7..6.4 -> "Prediabetes"
                value >= 6.5 -> "Elevated (>=6.5%)"
                else -> null
            }
            lower.contains("hemoglobin") || lower.contains("hb") -> when {
                value in 12.0..16.5 -> "Optimal"
                value < 12.0 -> "Low Hb"
                else -> "Elevated"
            }
            lower.contains("systolic") -> when {
                value < 120.0 -> "Optimal (<120)"
                value in 120.0..129.0 -> "Elevated (120-129)"
                value >= 130.0 -> "Stage 1 HTN"
                else -> null
            }
            else -> null
        }
    }
}
