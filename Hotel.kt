import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// Constantes do sistema
const val NOME_HOTEL = "Hotel Paraíso"
const val SENHA_ADMIN = "2678"
const val TOTAL_QUARTOS = 20
const val LIMITE_HOSPEDES = 15

// Modelos de dados
data class Hospede(
    val nome: String,
    val idade: Int
)

data class Quarto(
    val numero: Int,
    var ocupado: Boolean = false
)

data class Reserva(
    val hospede: Hospede,
    val quarto: Quarto,
    val diaria: Double,
    val dias: Int
) {
    val valorTotal: Double
        get() = diaria * dias
}

data class Evento(
    val empresa: String,
    val diaSemana: DayOfWeek,
    val horaInicio: Int,
    val duracaoHoras: Int,
    val auditorio: Auditorio,
    val quantidadePessoas: Int
) {
    val horaFim: Int
        get() = horaInicio + duracaoHoras

    val necessidadesBuffet: Buffet
        get() = Buffet.calcularNecessidades(quantidadePessoas)

    val quantidadeGarcons: Int
        get() = Math.ceil(quantidadePessoas / 12.0).toInt() + (duracaoHoras / 2)

    val custoGarcons: Double
        get() = quantidadeGarcons * 10.50 * duracaoHoras
}

data class Buffet(
    val cafeLitros: Double,
    val aguaLitros: Double,
    val salgados: Int
) {
    companion object {
        fun calcularNecessidades(pessoas: Int): Buffet {
            return Buffet(
                cafeLitros = 0.20 * pessoas,
                aguaLitros = 0.50 * pessoas,
                salgados = 7 * pessoas
            )
        }
    }

    val custoTotal: Double
        get() = (cafeLitros * 0.80) + (aguasLitros * 0.40) + (salgados * 0.34)
}

enum class Auditorio(val capacidade: Int, val nome: String, val cadeirasAdicionais: Int = 0) {
    LARANJA(150, "Auditório Laranja", 70),
    COLORADO(350, "Auditório Colorado");

    companion object {
        fun fromNumero(numero: Int): Auditorio? {
            return when(numero) {
                1 -> LARANJA
                2 -> COLORADO
                else -> null
            }
        }

        fun recomendarAuditorio(convidados: Int): Auditorio? {
            return when {
                convidados < 0 || convidados > COLORADO.capacidade -> null
                convidados <= LARANJA.capacidade + LARANJA.cadeirasAdicionais -> LARANJA
                else -> COLORADO
            }
        }
    }
}

// Serviços do hotel
class ServicoQuartos {
    private val quartos = List(TOTAL_QUARTOS) { Quarto(it + 1) }

    fun listarQuartos(): List<Quarto> = quartos.toList()

    fun buscarQuartoLivre(numero: Int): Quarto? {
        return quartos.getOrNull(numero - 1)?.takeUnless { it.ocupado }
    }

    fun reservarQuarto(quarto: Quarto) {
        quarto.ocupado = true
    }

    fun liberarQuarto(quarto: Quarto) {
        quarto.ocupado = false
    }

    fun statusQuartos(): String {
        return quartos.joinToString("; \n") {
            "${it.numero}- ${if (it.ocupado) "ocupado" else "livre"}"
        }
    }
}

class ServicoHospedes {
    private val hospedes = mutableListOf<Hospede>()

    fun cadastrarHospede(nome: String, idade: Int): Boolean {
        if (hospedes.size >= LIMITE_HOSPEDES) return false
        hospedes.add(Hospede(nome, idade))
        return true
    }

    fun buscarHospede(nome: String): Hospede? {
        return hospedes.find { it.nome.equals(nome, ignoreCase = true) }
    }

    fun listarHospedes(): List<Hospede> = hospedes.toList()

    fun calcularHospedagem(diaria: Double, hospedes: List<Hospede>): Triple<Double, Int, Int> {
        var total = 0.0
        var gratuidades = 0
        var meias = 0

        hospedes.forEach { hospede ->
            when {
                hospede.idade < 6 -> gratuidades++
                hospede.idade > 60 -> {
                    meias++
                    total += diaria / 2
                }
                else -> total += diaria
            }
        }

        return Triple(total, gratuidades, meias)
    }
}

class ServicoEventos {
    private val eventos = mutableListOf<Evento>()

    fun agendarEvento(evento: Evento): Boolean {
        if (!verificarDisponibilidade(evento.diaSemana, evento.horaInicio, evento.horaFim, evento.auditorio)) {
            return false
        }
        eventos.add(evento)
        return true
    }

    private fun verificarDisponibilidade(dia: DayOfWeek, horaInicio: Int, horaFim: Int, auditorio: Auditorio): Boolean {
        // Verificar horário de funcionamento
        val horarioFuncionamento = when {
            dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY -> 7..15
            else -> 7..23
        }

        if (horaInicio !in horarioFuncionamento || horaFim !in horarioFuncionamento) {
            return false
        }

        // Verificar conflito com outros eventos
        return eventos.none { evento ->
            evento.auditorio == auditorio &&
                    evento.diaSemana == dia &&
                    horaInicio < evento.horaFim && horaFim > evento.horaInicio
        }
    }

    fun listarEventos(): List<Evento> = eventos.toList()
}

// Interface do usuário
class InterfaceUsuario {
    private val servicoQuartos = ServicoQuartos()
    private val servicoHospedes = ServicoHospedes()
    private val servicoEventos = ServicoEventos()
    private var nomeUsuario: String = ""

    fun iniciar() {
        println("Bem-vindo ao $NOME_HOTEL")

        nomeUsuario = fazerLogin() ?: return

        println("\nBem-vindo ao $NOME_HOTEL, $nomeUsuario. É um imenso prazer ter você por aqui!")

        while (true) {
            exibirMenuPrincipal()
        }
    }

    private fun fazerLogin(): String? {
        print("Qual o seu nome? ")
        val nome = readlnOrNull()?.trim() ?: return null

        print("Qual a sua senha? ")
        val senha = readlnOrNull()?.trim() ?: return null

        return if (senha == SENHA_ADMIN) nome else null
    }

    private fun exibirMenuPrincipal() {
        println("\nMenu Principal:")
        println("1. Fazer Reserva")
        println("2. Cadastrar Hóspedes")
        println("3. Gerenciar Hóspedes")
        println("4. Agendar Eventos")
        println("5. Comparar Preços de Combustível")
        println("6. Orçamentos de Ar Condicionado")
        println("7. Sair")

        when (readlnOrNull()?.toIntOrNull()) {
            1 -> quantosQuartos()
            2 -> comoSoletra()
            3 -> comSOuZ()
            4 -> queHorasVocePode()
            5 -> alcoolOuGasolina()
            6 -> arPuroFinalmente()
            7 -> {
                println("Muito obrigado e até logo, $nomeUsuario.")
                System.exit(0)
            }
            else -> println("Opção inválida. Tente novamente.")
        }
    }

    private fun quantosQuartos() {
        println("\n--- Fazer Reserva ---")

        val diaria = lerDoublePositivo("Qual o valor padrão da diária?")
        val dias = lerInteiroPositivo("Quantas diárias serão necessárias?", 30)

        val valorTotal = diaria * dias
        println("O valor de $dias dias de hospedagem é de R$${String.format("%.2f", valorTotal)}")

        print("Qual o nome do hóspede? ")
        val nomeHospede = readlnOrNull()?.trim() ?: ""

        var quarto: Quarto?
        while (true) {
            val numeroQuarto = lerInteiroPositivo("Qual o quarto para reserva? (1 - $TOTAL_QUARTOS)?", TOTAL_QUARTOS)
            quarto = servicoQuartos.buscarQuartoLivre(numeroQuarto)

            if (quarto != null) {
                println("Quarto livre.")
                break
            } else {
                println("Quarto já está ocupado. Escolha outro.")
                println("Lista de quartos e suas ocupações: \n${servicoQuartos.statusQuartos()}")
            }
        }

        print("$nomeUsuario, você confirma a hospedagem para $nomeHospede por $dias dias para o quarto ${quarto.numero} por R$${String.format("%.2f", valorTotal)}? (S/N) ")
        if (readlnOrNull()?.equals("S", ignoreCase = true) == true) {
            servicoQuartos.reservarQuarto(quarto)
            servicoHospedes.cadastrarHospede(nomeHospede, 0) // Idade padrão 0 pois não foi solicitada
            println("$nomeUsuario, reserva efetuada para $nomeHospede.")
            println("Lista de quartos e suas ocupações: \n${servicoQuartos.statusQuartos()}")
        } else {
            println("Reserva cancelada.")
        }
    }

    private fun comoSoletra() {
        println("\n--- Cadastrar Hóspedes ---")

        val diaria = lerDoublePositivo("Qual o valor padrão da diária?")
        val hospedes = mutableListOf<Hospede>()

        while (true) {
            print("Qual o nome do hóspede? (Digite 'PARE' para encerrar): ")
            val nomeHospede = readlnOrNull()?.trim() ?: ""
            if (nomeHospede.equals("PARE", ignoreCase = true)) break

            val idade = lerInteiroPositivo("Qual a idade do hóspede?")
            hospedes.add(Hospede(nomeHospede, idade))

            when {
                idade < 6 -> println("$nomeHospede possui gratuidade.")
                idade > 60 -> println("$nomeHospede paga meia.")
                else -> println("$nomeHospede cadastrado(a) com sucesso.")
            }
        }

        val (total, gratuidades, meias) = servicoHospedes.calcularHospedagem(diaria, hospedes)
        println("$nomeUsuario, o valor total das hospedagens é: R$${String.format("%.2f", total)}; $gratuidades gratuidade(s); $meias meia(s)")
    }

    private fun comSOuZ() {
        println("\n--- Gerenciar Hóspedes ---")

        while (true) {
            println("\nOpções:")
            println("1. Cadastrar")
            println("2. Pesquisar")
            println("3. Listar")
            println("4. Sair")
            print("Escolha uma opção: ")

            when (readlnOrNull()?.toIntOrNull()) {
                1 -> {
                    if (servicoHospedes.listarHospedes().size >= LIMITE_HOSPEDES) {
                        println("Máximo de cadastros atingido.")
                        continue
                    }

                    print("Qual o nome do hóspede? ")
                    val nomeHospede = readlnOrNull()?.trim() ?: ""
                    servicoHospedes.cadastrarHospede(nomeHospede, 0) // Idade padrão 0
                    println("Hóspede $nomeHospede cadastrado(a) com sucesso!")
                }
                2 -> {
                    print("Qual o nome do hóspede? ")
                    val nomeHospede = readlnOrNull()?.trim() ?: ""
                    if (servicoHospedes.buscarHospede(nomeHospede) != null) {
                        println("Hóspede $nomeHospede foi encontrado(a)!")
                    } else {
                        println("Hóspede não encontrado(a).")
                    }
                }
                3 -> {
                    println("Lista de hóspedes:")
                    if (servicoHospedes.listarHospedes().isEmpty()) {
                        println("Nenhum hóspede cadastrado")
                    } else {
                        servicoHospedes.listarHospedes().forEach { println(it.nome) }
                    }
                }
                4 -> return
                else -> println("Opção inválida. Tente novamente.")
            }
        }
    }

    private fun queHorasVocePode() {
        println("\n--- Agendar Eventos ---")

        // Parte 1: Seleção do auditório
        val convidados = lerInteiroPositivo("Qual o número de convidados para o seu evento?")
        val auditorio = Auditorio.recomendarAuditorio(convidados) ?: run {
            println("Quantidade de convidados inválida ou superior à capacidade máxima.")
            return
        }

        if (auditorio == Auditorio.LARANJA && convidados > auditorio.capacidade) {
            val cadeirasExtras = convidados - auditorio.capacidade
            println("Use o auditório Laranja (inclua mais $cadeirasExtras cadeiras)")
        } else {
            println("Use o auditório ${auditorio.nome}")
        }

        println("Agora vamos ver a agenda do evento.")

        // Parte 2: Data e hora
        println("\nDias da semana:")
        DayOfWeek.values().forEachIndexed { index, dia ->
            println("${index + 1}. ${dia.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))}")
        }

        val numeroDia = lerInteiroPositivo("Dia do evento:", DayOfWeek.values().size)
        val diaSemana = DayOfWeek.values()[numeroDia - 1]

        // Definir horário de funcionamento baseado no dia
        val horarioFuncionamento = when {
            diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY -> 7..15
            else -> 7..23
        }

        val horaInicio = lerInteiroPositivo(
            "Hora de início (${horarioFuncionamento.first}-${horarioFuncionamento.last}):"
        ).takeIf { it in horarioFuncionamento } ?: run {
            println("Auditório indisponível nesse horário.")
            return
        }

        val duracao = lerInteiroPositivo("Duração do evento (horas):")
        val horaFim = horaInicio + duracao

        if (horaFim > horarioFuncionamento.last + 1) {
            println("O evento não pode terminar após o horário de funcionamento.")
            return
        }

        print("Qual o nome da empresa? ")
        val empresa = readlnOrNull()?.trim() ?: ""

        // Parte 3: Cálculo de garçons
        val garconsPorConvidados = Math.ceil(convidados / 12.0).toInt()
        val garconsPorDuracao = duracao / 2
        val totalGarcons = garconsPorConvidados + garconsPorDuracao
        val custoGarcons = totalGarcons * 10.50 * duracao

        println("\nSão necessários $totalGarcons garçons.")
        println("Custo com garçons: R$${String.format("%.2f", custoGarcons)}")
        println("Agora vamos calcular o buffet do hotel para o evento.")

        // Parte 4: Buffet
        val buffet = Buffet.calcularNecessidades(convidados)
        println("\nO evento precisará de:")
        println("- ${buffet.cafeLitros} litros de café")
        println("- ${buffet.aguasLitros} litros de água")
        println("- ${buffet.salgados} salgados")

        val custoBuffet = buffet.custoTotal
        val custoTotal = custoGarcons + custoBuffet

        // Relatório final
        println("\n--- Relatório do Evento ---")
        println("Evento no ${auditorio.nome}")
        println("Nome da Empresa: $empresa")
        println("Data: ${diaSemana.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))}, ${horaInicio}h às ${horaFim}h")
        println("Duração: $duracao horas")
        println("Quantidade de convidados: $convidados")
        println("Garçons necessários: $totalGarcons")
        println("Custo com garçons: R$${String.format("%.2f", custoGarcons)}")
        println("Custo com buffet: R$${String.format("%.2f", custoBuffet)}")
        println("---------------------------------")
        println("Custo total do evento: R$${String.format("%.2f", custoTotal)}")

        if (confirmarAcao("Deseja confirmar o agendamento deste evento?")) {
            val evento = Evento(empresa, diaSemana, horaInicio, duracao, auditorio, convidados)
            if (servicoEventos.agendarEvento(evento)) {
                println("$nomeUsuario, evento agendado com sucesso!")
            } else {
                println("Não foi possível agendar o evento. Horário indisponível.")
            }
        } else {
            println("Agendamento cancelado.")
        }
    }

    private fun alcoolOuGasolina() {
        println("\n--- Comparar Preços de Combustível ---")

        println("\nPosto Wayne Oil:")
        val alcoolWayne = lerDoublePositivo("Preço do álcool:")
        val gasolinaWayne = lerDoublePositivo("Preço da gasolina:")

        println("\nPosto Stark Petrol:")
        val alcoolStark = lerDoublePositivo("Preço do álcool:")
        val gasolinaStark = lerDoublePositivo("Preço da gasolina:")

        // Função para determinar o melhor combustível em um posto
        fun melhorCombustivel(alcool: Double, gasolina: Double): Pair<String, Double> {
            return if (alcool <= gasolina * 0.7) {
                "álcool" to alcool
            } else {
                "gasolina" to gasolina
            }
        }

        val (combustivelWayne, precoWayne) = melhorCombustivel(alcoolWayne, gasolinaWayne)
        val (combustivelStark, precoStark) = melhorCombustivel(alcoolStark, gasolinaStark)

        val litros = 42
        val custoWayne = precoWayne * litros
        val custoStark = precoStark * litros

        val (melhorPosto, melhorCombustivel, melhorPreco) = when {
            custoWayne < custoStark -> Pair("Wayne Oil", combustivelWayne, custoWayne)
            custoStark < custoWayne -> Pair("Stark Petrol", combustivelStark, custoStark)
            else -> Pair("Ambos postos", combustivelWayne, custoWayne)
        }

        println("\n$nomeUsuario, é mais barato abastecer com $melhorCombustivel no posto $melhorPosto")
        println("Custo para $litros litros: R$${String.format("%.2f", melhorPreco)}")
    }

    private fun arPuroFinalmente() {
        println("\n--- Orçamentos de Ar Condicionado ---")

        val orcamentos = mutableListOf<Triple<String, Double, Int>>() // (empresa, valor, quantidade)

        while (true) {
            print("\nNome da empresa (ou 'sair' para encerrar): ")
            val empresa = readlnOrNull()?.trim() ?: ""
            if (empresa.equals("sair", ignoreCase = true)) break

            val valorAparelho = lerDoublePositivo("Valor por aparelho:")
            val quantidade = lerInteiroPositivo("Quantidade de aparelhos:")
            val descontoPercentual = lerDoublePositivo("Percentual de desconto (%):")
            val minimoParaDesconto = lerInteiroPositivo("Mínimo para desconto:")

            var valorTotal = valorAparelho * quantidade

            if (quantidade >= minimoParaDesconto) {
                val desconto = valorTotal * (descontoPercentual / 100)
                valorTotal -= desconto
            }

            println("O serviço de $empresa custará R$${String.format("%.2f", valorTotal)}")
            orcamentos.add(Triple(empresa, valorTotal, quantidade))

            if (!confirmarAcao("Deseja informar novos dados?")) {
                break
            }
        }

        if (orcamentos.size >= 2) {
            val (melhorEmpresa, menorValor, _) = orcamentos.minByOrNull { it.second }!!
            println("\nO orçamento de menor valor é o de $melhorEmpresa por R$${String.format("%.2f", menorValor)}")
        } else {
            println("\nÉ necessário informar pelo menos dois orçamentos para comparação.")
        }
    }

    // Funções auxiliares
    private fun lerDoublePositivo(mensagem: String): Double {
        while (true) {
            print("$mensagem ")
            val valor = readlnOrNull()?.replace(',', '.')?.toDoubleOrNull()
            if (valor != null && valor >= 0) return valor
            println("Valor inválido. Digite um número positivo.")
        }
    }

    private fun lerInteiroPositivo(mensagem: String, maximo: Int = Int.MAX_VALUE): Int {
        while (true) {
            print("$mensagem ")
            val valor = readlnOrNull()?.toIntOrNull()
            if (valor != null && valor > 0 && valor <= maximo) return valor
            println("Valor inválido. Digite um número entre 1 e $maximo.")
        }
    }

    private fun confirmarAcao(mensagem: String): Boolean {
        print("$mensagem (S/N) ")
        return readlnOrNull()?.equals("S", ignoreCase = true) ?: false
    }
}

// Ponto de entrada do programa
fun main() {
    InterfaceUsuario().iniciar()
}