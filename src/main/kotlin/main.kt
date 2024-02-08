import de.fruiture.cor.ccs.CCSApplication
import de.fruiture.cor.ccs.CLI
import de.fruiture.cor.ccs.git.Git
import de.fruiture.cor.ccs.git.KommandSystemCaller

fun main(args: Array<String>) {
    CLI(CCSApplication(Git(KommandSystemCaller()))).main(args)
}