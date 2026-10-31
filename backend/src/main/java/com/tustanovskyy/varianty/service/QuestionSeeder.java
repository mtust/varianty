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
	public static final String LANGUAGE_UK = "uk";
	public static final String LANGUAGE_EN = "en";

	private final QuestionRepository questionRepository;

	public QuestionSeeder(QuestionRepository questionRepository) {
		this.questionRepository = questionRepository;
	}

	@Override
	public void run(String... args) {
		// Checked per language so a database seeded before English existed still gets it.
		if (questionRepository.countByLanguage(LANGUAGE_UK) == 0) {
			List<Question> all = new ArrayList<>();
			all.addAll(facts());
			all.addAll(quotes());
			questionRepository.saveAll(all);
		}
		if (questionRepository.countByLanguage(LANGUAGE_EN) == 0) {
			List<Question> all = new ArrayList<>();
			all.addAll(factsEn());
			all.addAll(quotesEn());
			questionRepository.saveAll(all);
		}
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

	// Same style as facts(): every answer is one or two words, never a number.
	private List<Question> factsEn() {
		return List.of(
				factEn(CATEGORY_FACTS, "Botanically speaking, a banana is a ___, but a strawberry is not.", "berry"),
				factEn(CATEGORY_FACTS, "Botanically speaking, a cucumber is a ___, not a vegetable.", "fruit"),
				factEn(CATEGORY_FACTS, "Ireland has no native ___, which legend credits to Saint Patrick.", "snakes"),
				factEn(CATEGORY_FACTS, "Sea otters sometimes wrap themselves in ___ while they sleep so they don't drift away.", "kelp"),
				factEn(CATEGORY_FACTS, "The largest iceberg ever recorded had a bigger surface area than the island of ___.", "Jamaica"),
				factEn(CATEGORY_FACTS, "An ostrich's eye is bigger than its ___.", "brain"),
				factEn(CATEGORY_FACTS, "Butterflies taste their food with their ___.", "feet"),
				factEn(CATEGORY_FACTS, "Octopus blood is ___ in color.", "blue"),
				factEn(CATEGORY_FACTS, "Wombat poop is shaped like a ___.", "cube"),
				factEn(CATEGORY_FACTS, "Honey found in ancient Egyptian tombs is still edible, because honey simply doesn't ___.", "spoil"),
				factEn(CATEGORY_FACTS, "Cows have best friends and get ___ when they are separated.", "stressed"),
				factEn(CATEGORY_FACTS, "A shrimp's heart is located in its ___.", "head"),
				factEn(CATEGORY_FACTS, "Sloths can hold their breath for longer than ___.", "dolphins"),
				factEn(CATEGORY_FACTS, "Cats can't taste anything ___.", "sweet"),
				factEn(CATEGORY_FACTS, "Elephants are among the few mammals that can't ___.", "jump"),
				factEn(CATEGORY_FACTS, "A crocodile can't stick out its ___.", "tongue"),
				factEn(CATEGORY_FACTS, "Polar bears are almost invisible to infrared cameras, except for their ___.", "noses"),
				factEn(CATEGORY_FACTS, "Koala fingerprints are almost identical to those of ___.", "humans"),
				factEn(CATEGORY_FACTS, "The national animal of Scotland is the ___.", "unicorn"),
				factEn(CATEGORY_FACTS, "The dot over a lowercase letter i is called a ___.", "tittle"),
				factEn(CATEGORY_FACTS, "A lightning bolt is hotter than the surface of the ___.", "sun"),
				factEn(CATEGORY_FACTS, "Guinness World Records named the ___ the most fearless animal in the world.", "honey badger"),
				factEn(CATEGORY_FACTS, "Cleopatra lived closer in time to the ___ landing than to the building of the Great Pyramid.", "Moon"));
	}

	// The original English wording of quotes(); uncertain sources are marked as attributed.
	private List<Question> quotesEn() {
		return List.of(
				factEn(CATEGORY_QUOTES, "Oscar Wilde wrote: \"I can resist everything except ___.\"", "temptation"),
				factEn(CATEGORY_QUOTES,
						"A line attributed to Albert Einstein: \"Two things are infinite: the universe and human ___; and I'm not sure about the universe.\"",
						"stupidity"),
				factEn(CATEGORY_QUOTES, "Groucho Marx said: \"I don't want to belong to any club that will accept me as a ___.\"", "member"),
				factEn(CATEGORY_QUOTES,
						"A line attributed to Winston Churchill: \"Success is stumbling from failure to failure with no loss of ___.\"",
						"enthusiasm"),
				factEn(CATEGORY_QUOTES, "Woody Allen is credited with: \"Eternity is a very long time, especially towards the ___.\"", "end"),
				factEn(CATEGORY_QUOTES, "A line attributed to Charlie Chaplin: \"A day without laughter is a day ___.\"", "wasted"),
				factEn(CATEGORY_QUOTES, "Mae West said: \"When I'm good, I'm very good, but when I'm bad, I'm ___.\"", "better"),
				factEn(CATEGORY_QUOTES, "Baseball legend Yogi Berra said: \"When you come to a fork in the road, ___ it.\"", "take"),
				factEn(CATEGORY_QUOTES, "Comedian Rodney Dangerfield's famous complaint: \"I don't get no ___.\"", "respect"),
				factEn(CATEGORY_QUOTES, "Benjamin Franklin wrote that nothing is certain except death and ___.", "taxes"),
				factEn(CATEGORY_QUOTES, "Albert Einstein said: \"Imagination is more important than ___.\"", "knowledge"),
				factEn(CATEGORY_QUOTES, "Salvador Dali said: \"Have no fear of perfection, you'll never ___ it.\"", "reach"),
				factEn(CATEGORY_QUOTES,
						"An old saying often credited to Mark Twain: \"Better to remain silent and be thought a fool than to speak and remove all ___.\"",
						"doubt"),
				factEn(CATEGORY_QUOTES, "Comedian Steven Wright joked: \"I intend to live forever. So far, so ___.\"", "good"));
	}

	private Question fact(String category, String fact, String correctAnswer) {
		return question(category, LANGUAGE_UK, fact, correctAnswer);
	}

	private Question factEn(String category, String fact, String correctAnswer) {
		return question(category, LANGUAGE_EN, fact, correctAnswer);
	}

	private Question question(String category, String language, String fact, String correctAnswer) {
		return Question.builder().fact(fact).correctAnswer(correctAnswer).category(category).language(language).build();
	}
}
