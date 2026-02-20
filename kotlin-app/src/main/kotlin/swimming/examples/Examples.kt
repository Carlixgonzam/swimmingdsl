package swimming.examples

data class DslExample(val name: String, val code: String)

val DSL_EXAMPLES = listOf(
    DslExample(
        name = "Sesión Básica",
        code = """session morning {
  swim 400 m freestyle easy pace 120
  4 x swim 100 m freestyle hard pace 90 rest 20 s
  swim 200 m backstroke moderate pace 130
  3 x kick 50 m hard rest 15 s
}"""
    ),
    DslExample(
        name = "Sesión Estructurada",
        code = """session advanced {
  warmup {
    swim 400 m freestyle easy pace 120
    swim 200 m backstroke easy pace 130
  }
  
  main {
    8 x swim 100 m freestyle hard pace 75 rest 15 s
    4 x swim 200 m backstroke moderate pace 110 rest 30 s
  }
  
  cooldown {
    swim 200 m easy pace 140
    kick 100 m easy
  }
}"""
    ),
    DslExample(
        name = "Con Equipamiento",
        code = """session withEquipment {
  warmup {
    swim 300 m easy pace 120 with fins
  }
  
  main {
    swim 200 m freestyle moderate with paddles
    kick 100 m hard with board
    swim 150 m easy with pullbuoy
  }
  
  cooldown {
    swim 200 m easy with snorkel
  }
}"""
    ),
    DslExample(
        name = "Sesión de Técnica",
        code = """session technique {
  warmup {
    swim 400 m easy pace 120
  }
  
  main {
    drill catchup 200 m easy
    drill fingertip 200 m easy
    4 x drill sculling 50 m easy rest 20 s
    drill onesided 200 m moderate
  }
  
  cooldown {
    swim 200 m easy pace 130
  }
}"""
    )
)
