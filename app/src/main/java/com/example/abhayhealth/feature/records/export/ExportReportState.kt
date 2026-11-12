package com.example.abhayhealth.feature.records.export

import java.io.File

sealed interface ExportReportState {
    object Idle : ExportReportState
    object Generating : ExportReportState
    data class Success(val file: File, val message: String = "Medical report generated successfully!") : ExportReportState
    data class Error(val message: String) : ExportReportState
}
