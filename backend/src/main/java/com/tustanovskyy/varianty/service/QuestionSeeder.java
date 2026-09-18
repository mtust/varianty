package com.tustanovskyy.varianty.service;

import com.tustanovskyy.varianty.domain.entity.Question;
import com.tustanovskyy.varianty.repository.QuestionRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class QuestionSeeder implements CommandLineRunner {

	public static final String CATEGORY_FACTS = "facts";
	public static final String CATEGORY_QUOTES = "quotes";

	private final QuestionRepository questionRepository;

	public QuestionSeeder(QuestionRepository questionRepository) {
		this.questionRepository = questionRepository;
	}

	@Override
	public void run(String... args) {
		if (questionRepository.count() > 0) {
			return;
		}
		List<Question> all = new ArrayList<>();
		all.addAll(facts());
		all.addAll(quotes());
		questionRepository.saveAll(all);
	}

	// Weird, non-obvious, funny facts — every answer is one or two words, never a number.
	private List<Question> facts() {
		return List.of(
				fact(CATEGORY_FACTS, "Банан — це ботанічно різновид ___, а не фрукт.", "ягоди"),
				fact(CATEGORY_FACTS, "Огірок — це ботанічно ___, а не овоч.", "фрукт"),
				fact(CATEGORY_FACTS, "В Ісландії немає жодного ___ (виду тварин), який водиться майже скрізь у світі.", "комара"),
				fact(CATEGORY_FACTS, "Морська видра під час сну тримається за лапи іншої видри або обмотується ___, щоб не запливти геть.", "водоростями"),
				fact(CATEGORY_FACTS, "Об'єм найбільшого зафіксованого айсберга перевищував площу ___ (країни).", "Ямайки"),
				fact(CATEGORY_FACTS, "У страуса очі більші за його ___.", "мозок"),
				fact(CATEGORY_FACTS, "Метелики куштують їжу ___.", "лапками"),
				fact(CATEGORY_FACTS, "Кров восьминога має ___ колір.", "блакитний"),
				fact(CATEGORY_FACTS, "Какашки вомбата мають форму ___.", "куба"),
				fact(CATEGORY_FACTS, "Мед, знайдений у єгипетських гробницях, досі їстівний, бо він просто не вміє ___.", "псуватися"),
				fact(CATEGORY_FACTS, "Корови мають найкращих друзів і сильно ___, коли їх розлучають.", "сумують"),
				fact(CATEGORY_FACTS, "Серце креветки міститься в її ___.", "голові"),
				fact(CATEGORY_FACTS, "Лінивці можуть затримувати подих довше, ніж ___.", "дельфіни"),
				fact(CATEGORY_FACTS, "Коти фізично не здатні відчувати смак ___.", "солодкого"),
				fact(CATEGORY_FACTS, "Слони — єдині ссавці у світі, які не вміють ___.", "стрибати"),
				fact(CATEGORY_FACTS, "Крокодил не може висунути ___.", "язик"),
				fact(CATEGORY_FACTS, "Білих ведмедів майже неможливо побачити на інфрачервоній камері — крім їхнього ___.", "носа"),
				fact(CATEGORY_FACTS, "Відбитки пальців коали майже ідентичні відбиткам ___.", "людини"),
				fact(CATEGORY_FACTS, "Національною твариною Шотландії офіційно є ___.", "єдиноріг"),
				fact(CATEGORY_FACTS, "Крапка над літерою «і» має власну назву — ___.", "тіттл"),
				fact(CATEGORY_FACTS, "Блискавка під час грози гарячіша, ніж поверхня ___.", "сонця"),
				fact(CATEGORY_FACTS, "Найбезстрашнішою твариною світу за версією Книги рекордів Гіннеса визнано ___.", "медоїда"),
				fact(CATEGORY_FACTS, "Клеопатра жила ближче за часом до польоту на ___, ніж до будівництва піраміди Хеопса.", "Місяць"));
	}

	// Funny quotes from famous people and comedians — the blank is one key word from the quote.
	private List<Question> quotes() {
		return List.of(
				fact(CATEGORY_QUOTES, "Оскар Вайльд казав: «Я можу опиратися всьому, окрім ___».", "спокуси"),
				fact(CATEGORY_QUOTES,
						"Альберт Ейнштейн жартував: «Є тільки дві нескінченні речі: Всесвіт і людська ___. Хоча щодо Всесвіту я не зовсім впевнений».",
						"дурість"),
				fact(CATEGORY_QUOTES,
						"Комік Ґраучо Маркс казав: «Я нізащо не вступив би в клуб, який погодився б прийняти в члени такого, як ___».", "я"),
				fact(CATEGORY_QUOTES, "Вінстон Черчилль казав: «Успіх — це вміння крокувати від невдачі до невдачі, не втрачаючи ___».",
						"ентузіазму"),
				fact(CATEGORY_QUOTES, "Комік Вуді Аллен жартував: «Вічність триває дуже довго, особливо ближче до ___».", "кінця"),
				fact(CATEGORY_QUOTES, "Чарлі Чаплін казав: «Один день без сміху — це втрачений ___».", "день"),
				fact(CATEGORY_QUOTES, "Актриса Мей Вест жартувала: «Коли я хороша, я дуже хороша. Але коли я погана, я ще ___».", "краща"),
				fact(CATEGORY_QUOTES, "Комік Йогі Берра казав: «Якщо дійшов до розвилки на дорозі — ___ її».", "бери"),
				fact(CATEGORY_QUOTES, "Комік Родні Дейнджерфілд жартував про своє життя: «Я не отримую жодної ___».", "поваги"),
				fact(CATEGORY_QUOTES, "Бенджамін Франклін казав: «Нічого не можна вважати певним, окрім смерті і ___».", "податків"),
				fact(CATEGORY_QUOTES, "Альберт Ейнштейн також казав: «Уява важливіша, ніж ___».", "знання"),
				fact(CATEGORY_QUOTES, "Сальвадор Далі казав: «Не бійся досконалості — тобі однаково її не ___».", "досягти"),
				fact(CATEGORY_QUOTES, "Авраамові Лінкольну приписують фразу: «Краще мовчати і здаватися дурнем, ніж говорити і розвіяти всі ___».",
						"сумніви"),
				fact(CATEGORY_QUOTES, "Комік Стівен Райт жартував: «Я маю намір жити вічно. Поки що ___».", "непогано"));
	}

	private Question fact(String category, String fact, String correctAnswer) {
		return Question.builder().fact(fact).correctAnswer(correctAnswer).category(category).build();
	}
}
