package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.dto.AccountMoneyTransferDto;
import com.db.awmd.challenge.exception.TransferMoneyAccountException;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;

    private final NotificationService notificationService;

    private final Map<String, Object> processingAccountIds = new ConcurrentHashMap<>();

    @Autowired
    public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
        this.accountsRepository = accountsRepository;
        this.notificationService = notificationService;
    }

    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    public Account getAccount(String accountId) {
        return this.accountsRepository.getAccount(accountId);
    }

    public void transferMoney(AccountMoneyTransferDto accountMoneyTransferDto) {
        try {
            if (transferBetweenAccountsAreInProgress(accountMoneyTransferDto)) {
                Thread.currentThread().wait(200L);
                transferMoney(accountMoneyTransferDto);
            }
            saveInprogressAccounts(accountMoneyTransferDto);
            Account accountFrom = accountsRepository.getAccount(accountMoneyTransferDto.getAccountFromId());
            validate(accountMoneyTransferDto, accountFrom);
            Account accountTo = accountsRepository.getAccount(accountMoneyTransferDto.getAccountToId());

            transfer(accountFrom, accountTo, accountMoneyTransferDto.getAmount());

            deleteInProgressAccounts(accountMoneyTransferDto);

            notificate(accountFrom, accountTo, accountMoneyTransferDto.getAmount());
        } catch (Exception exception) {
            deleteInProgressAccounts(accountMoneyTransferDto);
            String message = String.format("error transferMoney with info =%s", accountMoneyTransferDto);
            log.error(message, accountMoneyTransferDto);
            throw new TransferMoneyAccountException(message, exception);
        }
    }

    private void deleteInProgressAccounts(AccountMoneyTransferDto accountMoneyTransferDto) {
        processingAccountIds.remove(accountMoneyTransferDto.getAccountFromId());
        processingAccountIds.remove(accountMoneyTransferDto.getAccountToId());
    }

    private void saveInprogressAccounts(AccountMoneyTransferDto accountMoneyTransferDto) {
        processingAccountIds.put(accountMoneyTransferDto.getAccountFromId(), "");
        processingAccountIds.put(accountMoneyTransferDto.getAccountToId(), "");
    }

    private boolean transferBetweenAccountsAreInProgress(AccountMoneyTransferDto accountMoneyTransferDto) {
        return processingAccountIds.containsKey(accountMoneyTransferDto.getAccountFromId())
                || processingAccountIds.containsKey(accountMoneyTransferDto.getAccountToId());
    }

    private void notificate(Account accountFrom, Account accountTo, BigDecimal amount) {

        notificationService.notifyAboutTransfer(accountTo, String.format("transfered amount =%s from accId=%s",
                                                                         amount, accountFrom.getAccountId()
        ));
        notificationService.notifyAboutTransfer(accountFrom, String.format("transfered amount =%s to accId=%s",
                                                                           amount, accountTo.getAccountId()
        ));
    }

    private void transfer(Account accountFrom, Account accountTo, BigDecimal amount) {
        accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
        accountTo.setBalance(accountTo.getBalance().add(amount));
    }

    private void validate(AccountMoneyTransferDto accountMoneyTransferDto, Account accountFrom) {
        if (accountFrom.getBalance().compareTo(accountMoneyTransferDto.getAmount()) < 0) {
            throw new TransferMoneyAccountException("account do not has such money.");
        }
    }
}

