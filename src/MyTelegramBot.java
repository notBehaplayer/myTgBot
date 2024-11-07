import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.*;

public class MyTelegramBot extends TelegramLongPollingBot {
    private final Map<String, Boolean> usersDB = new HashMap<>();
    private final Map<String, Integer> userBalances = new HashMap<>();
    private static final String BOT_TOKEN = "7342253452:AAERN4KtHZDQwgex2MTEcd4QVoo-FGBCLns";
    private static final String BOT_USERNAME = "bpsExampleBot";
    private static final String[][] commandsList = {
            {"/start", "Запуск бота"},
            {"/help", "Список команд"},
            {"/signin", "Войти в аккаунт"},
            {"/signup", "Авторизоваться"},
            {"/balance", "Показать баланс"},
            {"/deposit", "Пополнить баланс"},
            {"/withdraw", "Снять средства"}
    };

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String userMessage = message.getText().toLowerCase();
            Long chatId = message.getChatId();
            String senderUsername = message.getFrom().getUserName();
            String senderFN = message.getFrom().getFirstName();

            switch (userMessage.split(" ")[0]) {
                case "/start":
                case "выход":
                    sendTextMessage(chatId, "Привет! Я бот от @behaplayer. Введите /help для списка команд.");
                    startMenuButtons(chatId);
                    break;

                case "/help":
                case "помощь":
                    showHelp(chatId);
                    break;

                case "/signin":
                case "войти":
                    signIn(chatId, senderUsername, senderFN);
                    break;

                case "/signup":
                case "авторизоваться":
                    signUp(chatId, senderUsername);
                    break;

                case "/balance":
                case "баланс":
                case "проверить баланс":
                    showBalance(chatId, senderUsername);
                    balanceMenuButtons(chatId);
                    break;

                case "/deposit":
                case "внести деньги":
                    handleDeposit(chatId, senderUsername, userMessage);
                    break;

                case "/withdraw":
                case "снять деньги":
                    handleWithdraw(chatId, senderUsername, userMessage);
                    break;

                default:
                    sendTextMessage(chatId, "Неверная команда. Введите /help для списка команд.");
            }
        }
    }

    private void showHelp(Long chatId) {
        StringBuilder helpText = new StringBuilder("Список команд:\n");
        for (String[] command : commandsList) {
            helpText.append(command[0]).append(" - ").append(command[1]).append(".\n");
        }
        sendTextMessage(chatId, helpText.toString());
    }

    private void signIn(Long chatId, String username, String firstName) {
        if (usersDB.getOrDefault(username, false)) {
            sendTextMessage(chatId, "Добро пожаловать, " + firstName + "!");
        } else {
            sendTextMessage(chatId, "Вы не авторизованы, введите /signup для авторизации.");
        }
    }

    private void signUp(Long chatId, String username) {
        if (!usersDB.containsKey(username)) {
            usersDB.put(username, true);
            userBalances.put(username, 0);
            sendTextMessage(chatId, "Вы успешно авторизованы! Теперь вы можете войти с помощью /signin.");
        } else {
            sendTextMessage(chatId, "Вы уже авторизованы.");
        }
    }

    private void showBalance(Long chatId, String username) {
        int balance = userBalances.getOrDefault(username, 0);
        sendTextMessage(chatId, "Ваш баланс: " + balance + "$");
    }

    private void handleDeposit(Long chatId, String username, String userMessage) {
        try {
            int amount = Integer.parseInt(userMessage.split(" ")[1]);  // Получаем сумму
            deposit(chatId, username, amount);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            sendTextMessage(chatId, "Пожалуйста, укажите корректную сумму для пополнения. Пример: /deposit 100");
        }
    }

    private void deposit(Long chatId, String username, int amount) {
        if (amount > 0 && usersDB.getOrDefault(username, false)) {
            userBalances.put(username, userBalances.getOrDefault(username, 0) + amount);
            sendTextMessage(chatId, "Вы пополнили баланс на " + amount + "$. Новый баланс: " + userBalances.get(username) + "$");
        } else if (amount <= 0) {
            sendTextMessage(chatId, "Сумма пополнения должна быть положительной.");
        } else {
            sendTextMessage(chatId, "Сначала авторизуйтесь с помощью /signin.");
        }
    }

    private void handleWithdraw(Long chatId, String username, String userMessage) {
        try {
            int amount = Integer.parseInt(userMessage.split(" ")[1]);  // Получаем сумму
            withdraw(chatId, username, amount);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            sendTextMessage(chatId, "Пожалуйста, укажите корректную сумму для снятия. Пример: /withdraw 100");
        }
    }

    private void withdraw(Long chatId, String username, int amount) {
        if (usersDB.getOrDefault(username, false)) {
            int currentBalance = userBalances.getOrDefault(username, 0);
            if (amount > 0 && currentBalance >= amount) {
                userBalances.put(username, currentBalance - amount);
                sendTextMessage(chatId, "Вы сняли " + amount + "$. Новый баланс: " + userBalances.get(username) + "$");
            } else if (amount <= 0) {
                sendTextMessage(chatId, "Сумма снятия должна быть положительной.");
            } else {
                sendTextMessage(chatId, "Недостаточно средств для снятия.");
            }
        } else {
            sendTextMessage(chatId, "Сначала авторизуйтесь с помощью /signin.");
        }
    }

    private void startMenuButtons(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите действие:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Войти");
        row1.add("Авторизоваться");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Помощь");
        row2.add("Баланс");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void balanceMenuButtons(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите действие:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Проверить баланс");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Внести деньги");
        row2.add("Снять деньги");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Выйти");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MyTelegramBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
