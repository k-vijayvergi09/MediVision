package com.samsung.android.medivision.data.util

object Prompts {
    val EXTRACT_MEDICINE_INFO = """
                Extract medicine information from this prescription and return ONLY a valid JSON response in the following exact format:

                {
                  "medicines": [
                    {
                      "name": "Medicine name",
                      "when_to_take": "Morning",
                      "frequency": 1
                    }
                  ]
                }

                CRITICAL RULES:
                1. "when_to_take" must be EXACTLY one of: "Morning", "Evening", or "Both"
                2. "frequency" must be a number: 1 (once daily) or 2 (twice daily)
                3. If dosage timing is not mentioned or unclear, use "Morning" as default
                4. If frequency is not mentioned or unclear, use 1 as default
                5. Return ONLY the JSON object, no additional text or explanation

                Common prescription patterns to recognize:
                - "OD" / "Once daily" / "1 time" = frequency: 1
                - "BD" / "BID" / "Twice daily" / "2 times" = frequency: 2
                - "Morning" / "AM" / "Before breakfast" = when_to_take: "Morning"
                - "Evening" / "PM" / "Before dinner" / "Night" = when_to_take: "Evening"
                - "Morning and evening" / "AM & PM" / "Twice" = when_to_take: "Both"

                If no medicines are found, return: {"medicines": []}
            """.trimIndent()

    val READ_PRESCRIPTION = """
        Please extract all medicine information from this medical prescription. 
        For each medicine, include:
        1. Name of the medicine
        2. Dosage instructions (e.g., 500mg, 1 tablet)
        3. Frequency (e.g., twice a day, before meals)
        4. Duration (if mentioned, e.g., for 5 days)
        
        Format the output in a json format. If no medicines are found, return empty json.
        Structure of json should only contain following fields:
        {
            "medicines": {
                "name": <Medicine name>,
                "dosage": <Dosage instructions>,
                "frequency": <Frequency>,
                "duration": <Duration>
            } 
        }
        Duration should hold one of three values - "Morning", "Afternoon", "Evening".
        Whatever information is present about duration in the prescription, try to map it to
        the above three categories.
    """.trimIndent()
}