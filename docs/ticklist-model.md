# TickList Model Diagram

Current backend model from `backend/src/main/java/com/riley/ticklist`.

```mermaid
classDiagram
  direction LR

  class Tick {
    Long id
    String climbName
    String climbId
    Discipline discipline
    TickType tickType
    User user
    String location
    String grade
    String rawGrade
    Double gradeValue
    String personalGrade
    Double stars
    Double climbHeight
    GradeSystem gradeSystem
    GradeMapping gradeMapping
    SourceApp sourceApp
    String externalId
    String sourceUrl
    String style
    RopeStyle ropeStyle
    LocalDate tickDate
    Integer attempts
    Integer pitches
    Double userStars
    String notes
    LocalDateTime createdAt
    LocalDateTime updatedAt
  }

  class User {
    Long id
    String firstName
    String lastName
    String email
    String password
  }

  class GradeMapping {
    Long id
    String rawGrade
    GradeSystem gradeSystem
    Discipline discipline
    Double systemOrder
    Double difficultyScore
    Double confidence
    Boolean active
  }

  class ImportBatch {
    Long id
    User user
    SourceApp sourceApp
    String originalFilename
    Integer totalRows
    Integer successfulRows
    Integer failedRows
    LocalDateTime importedAt
  }

  class TickType {
    <<enumeration>>
    SEND
    ATTEMPT
    CLEAN_TR
    UNKNOWN
  }

  class Discipline {
    <<enumeration>>
    BOULDER
    SPORT
    TRAD
    ICE
    MIXED
    AID
    GYM
    UNKNOWN
  }

  class GradeSystem {
    <<enumeration>>
    V_SCALE
    FONT
    YDS
    FRENCH_SPORT
    ICE_WI
    MIXED_M
    AID
    E_Grade
    UNKNOWN
  }

  class RopeStyle {
    <<enumeration>>
    FLASH
    ONSIGHT
    REDPOINT
    PINKPOINT
    REPEAT
    SOLO
    LRS
    FS
    OSFS
    DS
    FB
    DWS
    UNKNOWN
  }

  class SourceApp {
    <<enumeration>>
    MANUAL
    MOUNTAIN_PROJECT
    KAYA
    EIGHT_A
    UNKNOWN
  }

  Tick --> User : user many-to-one
  Tick --> GradeMapping : gradeMapping many-to-one
  ImportBatch --> User : user many-to-one

  Tick ..> TickType : tickType
  Tick ..> Discipline : discipline
  Tick ..> GradeSystem : gradeSystem
  Tick ..> RopeStyle : ropeStyle
  Tick ..> SourceApp : sourceApp

  GradeMapping ..> GradeSystem : gradeSystem
  GradeMapping ..> Discipline : discipline
  ImportBatch ..> SourceApp : sourceApp
```

Notes:

- `Tick`, `User`, `GradeMapping`, and `ImportBatch` are the current JPA entities.
- `Tick.user`, `Tick.gradeMapping`, and `ImportBatch.user` are the current `@ManyToOne` relationships.
- `User` maps to table `app_users`; the other entity table names use default JPA naming.
- `Climb` is not shown because there is no current `Climb` entity in the codebase yet.
- `Grade` is not shown because it is a package-private helper/value class, not a JPA entity and not currently linked from `Tick`.
