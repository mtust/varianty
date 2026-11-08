package com.tustanovskyy.varianty.service;

import com.tustanovskyy.varianty.domain.entity.Question;
import com.tustanovskyy.varianty.repository.QuestionRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
		// Inserts every question whose text isn't in the database yet, so questions added here
		// reach an already-seeded (production) database on the next deploy. Editing an existing
		// question's text therefore adds it as a new question; the old row has to be removed by hand.
		List<Question> uk = new ArrayList<>(facts());
		uk.addAll(quotes());
		insertMissing(LANGUAGE_UK, uk);
		List<Question> en = new ArrayList<>(factsEn());
		en.addAll(quotesEn());
		insertMissing(LANGUAGE_EN, en);
	}

	private void insertMissing(String language, List<Question> questions) {
		Set<String> known = questionRepository.findByLanguage(language).stream()
				.map(Question::getFact)
				.collect(Collectors.toCollection(HashSet::new));
		List<Question> missing = questions.stream().filter(q -> known.add(q.getFact())).toList();
		if (!missing.isEmpty()) {
			questionRepository.saveAll(missing);
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
				fact(CATEGORY_FACTS, "Клеопатра жила ближче за часом до польоту на ___, ніж до будівництва піраміди Хеопса.", "Місяць"),
				fact(CATEGORY_FACTS, "Равлик може проспати кілька років поспіль, якщо погода надто ___.", "суха"),
				fact(CATEGORY_FACTS, "У восьминога три ___.", "серця"),
				fact(CATEGORY_FACTS, "Фламінго рожеві через рачків і ___, якими вони харчуються.", "водорості"),
				fact(CATEGORY_FACTS, "У морських коників потомство виношують ___.", "самці"),
				fact(CATEGORY_FACTS, "Голубів у лабораторії навчили розрізняти картини Пікассо і ___.", "Моне"),
				fact(CATEGORY_FACTS, "Ейфелева вежа влітку стає трохи вищою через ___.", "спеку"),
				fact(CATEGORY_FACTS, "Доба на Венері триває довше, ніж венеріанський ___.", "рік"),
				fact(CATEGORY_FACTS, "Арахіс ботанічно не горіх, а близький родич ___.", "гороху"),
				fact(CATEGORY_FACTS, "Колібрі — єдині птахи, які вміють літати ___.", "назад"),
				fact(CATEGORY_FACTS, "Акули з'явилися на Землі раніше, ніж ___.", "дерева"),
				fact(CATEGORY_FACTS, "Оксфордський університет старший за імперію ___.", "ацтеків"),
				fact(CATEGORY_FACTS, "Компанія Nintendo починала як виробник ___.", "гральних карт"),
				fact(CATEGORY_FACTS, "Найкоротша війна в історії, між Британією та ___, тривала менше години.", "Занзібаром"),
				fact(CATEGORY_FACTS, "Першими живими істотами, яких навмисно запустили в космос, були ___.", "мухи"),
				fact(CATEGORY_FACTS, "Коали сплять майже цілу добу, бо листя ___ дає їм дуже мало енергії.", "евкаліпта"),
				fact(CATEGORY_FACTS, "Сова не може рухати ___, тому повертає всю голову.", "очима"),
				fact(CATEGORY_FACTS, "Під час залицяння самець пінгвіна дарує самиці ___.", "камінчик"),
				fact(CATEGORY_FACTS, "У морської зірки немає ні крові, ні ___.", "мозку"),
				fact(CATEGORY_FACTS, "Один вид медуз науковці називають біологічно ___.", "безсмертним"),
				fact(CATEGORY_FACTS, "«O'кей» походить від жартівливого неграмотного «oll korrect», тобто «все ___».", "правильно"),
				fact(CATEGORY_FACTS, "Гавайські острови щороку на кілька сантиметрів наближаються до ___.", "Японії"),
				fact(CATEGORY_FACTS, "Банани трохи ___ через калій, який у них міститься.", "радіоактивні"),
				fact(CATEGORY_FACTS, "Через мікроводорості сніг у горах буває рожевим і навіть пахне ___.", "кавуном"),
				fact(CATEGORY_FACTS, "Пінгвін Нільс Олав в Единбурзькому зоопарку має генеральське звання і титул ___.", "лицаря"),
				fact(CATEGORY_FACTS, "Наполеон насправді був не низьким, а ___ зросту для свого часу.", "середнього"),
				fact(CATEGORY_FACTS, "Справжні вікінги не носили шоломів із ___.", "рогами"),
				fact(CATEGORY_FACTS, "Пам'ять золотої рибки триває не три секунди, а кілька ___.", "місяців"),
				fact(CATEGORY_FACTS, "Шоколад небезпечний для ___ через речовину теобромін.", "собак"),
				fact(CATEGORY_FACTS, "Перша окультурена морква була здебільшого ___ кольору, а не помаранчевого.", "фіолетового"),
				fact(CATEGORY_FACTS, "У 1830-х роках кетчуп продавали в США як ___.", "ліки"),
				fact(CATEGORY_FACTS, "Тигр смугастий не лише шерстю, а й ___.", "шкірою"),
				fact(CATEGORY_FACTS, "Найближчі живі родичі носорогів — це ___.", "коні"),
				fact(CATEGORY_FACTS, "Найближчий живий родич китів — ___.", "бегемот"),
				fact(CATEGORY_FACTS, "Колись в Америці омарів вважали їжею для ___.", "в'язнів"),
				fact(CATEGORY_FACTS, "Кенгуру практично не вміє ходити ___.", "задки"),
				fact(CATEGORY_FACTS, "Щури, коли їх лоскочуть, ___.", "сміються"),
				fact(CATEGORY_FACTS, "Деякі австралійські черепахи під водою можуть дихати ___.", "задом"),
				fact(CATEGORY_FACTS, "Мова ротокас з Папуа Нової Гвінеї має один із найкоротших ___ у світі.", "алфавітів"),
				fact(CATEGORY_FACTS, "Слово «робот» уперше з'явилося в п'єсі чеського письменника Карела ___.", "Чапека"),
				fact(CATEGORY_FACTS, "Першим задокументованим комп'ютерним «багом» була справжня ___, що застрягла в реле.", "міль"),
				fact(CATEGORY_FACTS, "Ейфелеву вежу спочатку планували через двадцять років ___.", "розібрати"),
				fact(CATEGORY_FACTS, "Статуя Свободи спочатку була блискучого ___ кольору, а зеленою стала від окислення.", "мідного"),
				fact(CATEGORY_FACTS, "У Мони Лізи немає ___.", "брів"),
				fact(CATEGORY_FACTS, "На площі Святого Марка у Венеції заборонено годувати ___.", "голубів"),
				fact(CATEGORY_FACTS, "Бетховен написав свою Дев'яту симфонію, будучи майже повністю ___.", "глухим"),
				fact(CATEGORY_FACTS, "У Фінляндії проводять чемпіонат світу з перенесення ___.", "дружин"),
				fact(CATEGORY_FACTS, "Японський острів Окуносіма населений здебільшого ___.", "кроликами"),
				fact(CATEGORY_FACTS, "Австралія з заходу на схід ширша, ніж ___.", "Місяць"),
				fact(CATEGORY_FACTS, "Площа поверхні Плутона менша за площу ___ (країни).", "Росії"),
				fact(CATEGORY_FACTS, "Воду можна змусити одночасно кипіти і ___.", "замерзати"),
				fact(CATEGORY_FACTS, "Гаряча вода іноді замерзає швидше за холодну — це називають ефектом ___.", "Мпемби"),
				fact(CATEGORY_FACTS, "Ліва легеня людини менша за праву, щоб звільнити місце для ___.", "серця"),
				fact(CATEGORY_FACTS, "Найбільший орган людського тіла — це ___.", "шкіра"),
				fact(CATEGORY_FACTS, "Помідор, як і банан, ботанічно є ___.", "ягодою"));
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
				fact(CATEGORY_QUOTES, "Комік Стівен Райт жартував: «Я маю намір жити вічно. Поки що ___».", "непогано"),
				fact(CATEGORY_QUOTES, "Маркові Твену приписують фразу: «Чутки про мою смерть дещо ___».", "перебільшені"),
				fact(CATEGORY_QUOTES, "Маркові Твену приписують: «Кинути курити дуже легко. Я сам ___ це тисячу разів».", "робив"),
				fact(CATEGORY_QUOTES, "Марк Твен радив: «Ніколи не відкладай на завтра те, що можна так само добре зробити ___».", "післязавтра"),
				fact(CATEGORY_QUOTES, "Маркові Твену приписують: «Спершу з'ясуй факти, а потім можеш ___ їх як завгодно».", "перекручувати"),
				fact(CATEGORY_QUOTES, "Оскар Вайльд радив: «Завжди прощайте своїх ворогів — ніщо їх так не ___».", "дратує"),
				fact(CATEGORY_QUOTES, "Оскарові Вайльду приписують: «Будь собою — всі інші вже ___».", "зайняті"),
				fact(CATEGORY_QUOTES, "Оскар Вайльд писав: «Я такий розумний, що часом не розумію жодного ___ з того, що кажу».", "слова"),
				fact(CATEGORY_QUOTES, "Ґраучо Марксу приписують: «Поза собакою книга — найкращий друг людини. Всередині собаки надто ___, щоб читати».", "темно"),
				fact(CATEGORY_QUOTES, "Ґраучо Маркс сказав господині: «Я чудово провів вечір, але це був не ___».", "цей"),
				fact(CATEGORY_QUOTES, "Ґраучо Марксу приписують: «Це мої принципи. Якщо вони вам не подобаються — у мене є ___».", "інші"),
				fact(CATEGORY_QUOTES, "Черчиллю приписують відповідь леді Астор: «Якби я був вашим чоловіком, я б його ___».", "випив"),
				fact(CATEGORY_QUOTES, "Черчиллю приписують: «Мене дуже легко задовольнити — ___».", "найкращим"),
				fact(CATEGORY_QUOTES, "Вуді Аллен жартував: «Я не боюся смерті. Я просто не хочу бути ___, коли це станеться».", "там"),
				fact(CATEGORY_QUOTES, "Комік Мітч Хедберг жартував: «Я не спав десять днів, бо це було б надто ___».", "довго"),
				fact(CATEGORY_QUOTES, "Стівен Райт жартував: «У мене велика колекція мушель. Я тримаю її розкиданою по ___ всього світу».", "пляжах"),
				fact(CATEGORY_QUOTES, "Стівен Райт також казав: «Куди завгодно можна дійти пішки, якщо маєш досить ___».", "часу"),
				fact(CATEGORY_QUOTES, "Бернарду Шоу приписують: «Молодість — чудова річ. Шкода, що її марнують на ___».", "молодих"),
				fact(CATEGORY_QUOTES, "Джеррі Сайнфелд жартував: на похороні людина радше лежатиме в труні, ніж виголошуватиме ___.", "промову"),
				fact(CATEGORY_QUOTES, "Дуглас Адамс писав: «Я люблю дедлайни. Люблю той свист, з яким вони ___ повз».", "пролітають"),
				fact(CATEGORY_QUOTES, "Йогі Берра казав про ресторан: «Туди вже ніхто не ходить — там надто ___».", "людно"),
				fact(CATEGORY_QUOTES, "Йогі Берра також казав: «Це дежавю ___».", "знову"),
				fact(CATEGORY_QUOTES, "Вілл Роджерс казав: «Усе смішно, поки це відбувається з ___».", "іншими"),
				fact(CATEGORY_QUOTES, "Комікеса Філліс Діллер жартувала: «Хатня робота вас не вб'є, але навіщо ___?»", "ризикувати"),
				fact(CATEGORY_QUOTES, "Сократу приписують: «Неодмінно одружуйся. З доброю дружиною будеш щасливим, з поганою — станеш ___».", "філософом"),
				fact(CATEGORY_QUOTES, "Мей Вест казала: «Зазвичай я уникаю спокус — якщо тільки не можу перед ними ___».", "встояти"),
				fact(CATEGORY_QUOTES, "Мей Вест також казала: «Забагато хорошого — це ___».", "чудово"));
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
				factEn(CATEGORY_FACTS, "Cleopatra lived closer in time to the ___ landing than to the building of the Great Pyramid.", "Moon"),
				factEn(CATEGORY_FACTS, "A snail can sleep for years at a time if the weather is too ___.", "dry"),
				factEn(CATEGORY_FACTS, "An octopus has three ___.", "hearts"),
				factEn(CATEGORY_FACTS, "Flamingos are pink because of the shrimp and ___ they eat.", "algae"),
				factEn(CATEGORY_FACTS, "In seahorses, it's the ___ who get pregnant.", "males"),
				factEn(CATEGORY_FACTS, "In a lab experiment, pigeons learned to tell paintings by Picasso from paintings by ___.", "Monet"),
				factEn(CATEGORY_FACTS, "The Eiffel Tower grows slightly taller in summer because of the ___.", "heat"),
				factEn(CATEGORY_FACTS, "A day on Venus lasts longer than a Venusian ___.", "year"),
				factEn(CATEGORY_FACTS, "Botanically, a peanut is not a nut but a close relative of the ___.", "pea"),
				factEn(CATEGORY_FACTS, "Hummingbirds are the only birds that can fly ___.", "backwards"),
				factEn(CATEGORY_FACTS, "Sharks have existed on Earth for longer than ___.", "trees"),
				factEn(CATEGORY_FACTS, "Oxford University is older than the ___ Empire.", "Aztec"),
				factEn(CATEGORY_FACTS, "Nintendo started out as a company making ___.", "playing cards"),
				factEn(CATEGORY_FACTS, "The shortest war in history, between Britain and ___, lasted less than an hour.", "Zanzibar"),
				factEn(CATEGORY_FACTS, "The first living creatures deliberately sent into space were ___.", "fruit flies"),
				factEn(CATEGORY_FACTS, "Koalas sleep most of the day because ___ leaves give them very little energy.", "eucalyptus"),
				factEn(CATEGORY_FACTS, "An owl can't move its ___, so it turns its whole head instead.", "eyes"),
				factEn(CATEGORY_FACTS, "When courting, a male penguin presents the female with a ___.", "pebble"),
				factEn(CATEGORY_FACTS, "A starfish has no blood and no ___.", "brain"),
				factEn(CATEGORY_FACTS, "Scientists call one species of jellyfish biologically ___.", "immortal"),
				factEn(CATEGORY_FACTS, "\"OK\" comes from a jokey misspelling, \"oll korrect\", meaning \"all ___\".", "correct"),
				factEn(CATEGORY_FACTS, "Every year the Hawaiian Islands move a few centimeters closer to ___.", "Japan"),
				factEn(CATEGORY_FACTS, "Bananas are slightly ___ because of the potassium they contain.", "radioactive"),
				factEn(CATEGORY_FACTS, "Thanks to tiny algae, mountain snow can turn pink and even smell like ___.", "watermelon"),
				factEn(CATEGORY_FACTS, "Nils Olav, a penguin at Edinburgh Zoo, holds a general's rank and the title of ___.", "knight"),
				factEn(CATEGORY_FACTS, "Napoleon wasn't actually short; he was of ___ height for his time.", "average"),
				factEn(CATEGORY_FACTS, "Real Vikings never wore helmets with ___.", "horns"),
				factEn(CATEGORY_FACTS, "A goldfish's memory lasts not three seconds but several ___.", "months"),
				factEn(CATEGORY_FACTS, "Chocolate is dangerous for ___ because of a substance called theobromine.", "dogs"),
				factEn(CATEGORY_FACTS, "The first cultivated carrots were mostly ___, not orange.", "purple"),
				factEn(CATEGORY_FACTS, "In the 1830s, ketchup was sold in the United States as ___.", "medicine"),
				factEn(CATEGORY_FACTS, "A tiger's stripes are not only on its fur but also on its ___.", "skin"),
				factEn(CATEGORY_FACTS, "The closest living relatives of rhinos are ___.", "horses"),
				factEn(CATEGORY_FACTS, "The closest living relative of the whale is the ___.", "hippo"),
				factEn(CATEGORY_FACTS, "In colonial America, lobster was considered food for ___.", "prisoners"),
				factEn(CATEGORY_FACTS, "A kangaroo can hardly walk ___.", "backwards"),
				factEn(CATEGORY_FACTS, "When you tickle a rat, it ___.", "laughs"),
				factEn(CATEGORY_FACTS, "Some Australian turtles can breathe underwater through their ___.", "butts"),
				factEn(CATEGORY_FACTS, "The Rotokas language of Papua New Guinea has one of the shortest ___ in the world.", "alphabets"),
				factEn(CATEGORY_FACTS, "The word \"robot\" first appeared in a play by the Czech writer Karel ___.", "Capek"),
				factEn(CATEGORY_FACTS, "The first documented computer \"bug\" was a real ___ stuck in a relay.", "moth"),
				factEn(CATEGORY_FACTS, "The Eiffel Tower was originally supposed to be ___ after twenty years.", "dismantled"),
				factEn(CATEGORY_FACTS, "The Statue of Liberty was originally a shiny ___ color and turned green from oxidation.", "copper"),
				factEn(CATEGORY_FACTS, "The Mona Lisa has no ___.", "eyebrows"),
				factEn(CATEGORY_FACTS, "It is forbidden to feed the ___ in St Mark's Square in Venice.", "pigeons"),
				factEn(CATEGORY_FACTS, "Beethoven composed his Ninth Symphony while almost completely ___.", "deaf"),
				factEn(CATEGORY_FACTS, "Finland hosts the world championship in ___ carrying.", "wife"),
				factEn(CATEGORY_FACTS, "The Japanese island of Okunoshima is mostly inhabited by ___.", "rabbits"),
				factEn(CATEGORY_FACTS, "From west to east, Australia is wider than the ___.", "Moon"),
				factEn(CATEGORY_FACTS, "Pluto's surface area is smaller than the area of ___ (the country).", "Russia"),
				factEn(CATEGORY_FACTS, "Water can be made to boil and ___ at the same time.", "freeze"),
				factEn(CATEGORY_FACTS, "Hot water sometimes freezes faster than cold water; this is called the ___ effect.", "Mpemba"),
				factEn(CATEGORY_FACTS, "The human left lung is smaller than the right one to make room for the ___.", "heart"),
				factEn(CATEGORY_FACTS, "The largest organ of the human body is the ___.", "skin"),
				factEn(CATEGORY_FACTS, "Like the banana, a tomato is botanically a ___.", "berry"));
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
				factEn(CATEGORY_QUOTES, "Comedian Steven Wright joked: \"I intend to live forever. So far, so ___.\"", "good"),
				factEn(CATEGORY_QUOTES, "Mark Twain wrote: \"The report of my death was an ___.\"", "exaggeration"),
				factEn(CATEGORY_QUOTES, "A line attributed to Mark Twain: \"Quitting smoking is easy. I've done it a thousand ___.\"", "times"),
				factEn(CATEGORY_QUOTES, "Mark Twain advised: \"Never put off till tomorrow what may be done the day after tomorrow just as ___.\"", "well"),
				factEn(CATEGORY_QUOTES, "A line attributed to Mark Twain: \"Get your facts first, and then you can ___ them as you please.\"", "distort"),
				factEn(CATEGORY_QUOTES, "Oscar Wilde advised: \"Always forgive your enemies; nothing ___ them so much.\"", "annoys"),
				factEn(CATEGORY_QUOTES, "A line attributed to Oscar Wilde: \"Be yourself; everyone else is already ___.\"", "taken"),
				factEn(CATEGORY_QUOTES, "Oscar Wilde wrote: \"I am so clever that sometimes I don't understand a single ___ of what I am saying.\"", "word"),
				factEn(CATEGORY_QUOTES, "A line attributed to Groucho Marx: \"Outside of a dog, a book is man's best friend. Inside of a dog it's too ___ to read.\"", "dark"),
				factEn(CATEGORY_QUOTES, "Groucho Marx told a hostess: \"I've had a perfectly wonderful evening, but this wasn't ___.\"", "it"),
				factEn(CATEGORY_QUOTES, "A line attributed to Groucho Marx: \"Those are my principles, and if you don't like them... well, I have ___.\"", "others"),
				factEn(CATEGORY_QUOTES, "Churchill's attributed reply to Lady Astor: \"Madam, if I were your husband, I would ___ it.\"", "drink"),
				factEn(CATEGORY_QUOTES, "A line attributed to Winston Churchill: \"I am easily satisfied with the very ___.\"", "best"),
				factEn(CATEGORY_QUOTES, "Woody Allen joked: \"I'm not afraid of death; I just don't want to be ___ when it happens.\"", "there"),
				factEn(CATEGORY_QUOTES, "Comedian Mitch Hedberg joked: \"I haven't slept for ten days, because that would be too ___.\"", "long"),
				factEn(CATEGORY_QUOTES, "Steven Wright joked: \"I have a large seashell collection, which I keep scattered on ___ all over the world.\"", "beaches"),
				factEn(CATEGORY_QUOTES, "Steven Wright also said: \"Everywhere is within walking distance if you have the ___.\"", "time"),
				factEn(CATEGORY_QUOTES, "A line attributed to George Bernard Shaw: \"Youth is wasted on the ___.\"", "young"),
				factEn(CATEGORY_QUOTES, "Jerry Seinfeld joked that at a funeral, most people would rather be in the casket than giving the ___.", "eulogy"),
				factEn(CATEGORY_QUOTES, "Douglas Adams wrote: \"I love deadlines. I love the whooshing noise they make as they go ___.\"", "by"),
				factEn(CATEGORY_QUOTES, "Yogi Berra said about a restaurant: \"Nobody goes there anymore. It's too ___.\"", "crowded"),
				factEn(CATEGORY_QUOTES, "Yogi Berra also said: \"It's deja vu all over ___.\"", "again"),
				factEn(CATEGORY_QUOTES, "Will Rogers said: \"Everything is funny, as long as it's happening to somebody ___.\"", "else"),
				factEn(CATEGORY_QUOTES, "Comedian Phyllis Diller joked: \"Housework can't kill you, but why take a ___?\"", "chance"),
				factEn(CATEGORY_QUOTES, "A line attributed to Socrates: \"By all means marry. If you get a good wife, you'll be happy; if you get a bad one, you'll become a ___.\"", "philosopher"),
				factEn(CATEGORY_QUOTES, "Mae West said: \"I generally avoid temptation unless I can't ___ it.\"", "resist"),
				factEn(CATEGORY_QUOTES, "Mae West also said: \"Too much of a good thing can be ___.\"", "wonderful"));
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
