#!/usr/bin/env python
import argparse
import requests
import sys
import time
import os
from random import randint
import email, getpass, imaplib, re

RUNESCAPE_REGISTER_URL = 'https://secure.runescape.com/m=account-creation/g=oldscape/create_account'
RUNESCAPE_RECAPTCHA_KEY = '6Lcsv3oUAAAAAGFhlKrkRb029OHio098bbeyi_Hv'
CAPTCHA_URL = 'http://2captcha.com/'
CAPTCHA_REQ_URL = CAPTCHA_URL + 'in.php'
CAPTCHA_RES_URL = CAPTCHA_URL + 'res.php'
CAPTCHA_API_KEY = '4935affd16c15fb4100e8813cdccfab6'


def verify_email(s, sleep=60):
    server_name = "imap.gmail.com"
    username = "milleja115"
    password = "Xb32y0x5!"
    subject = 'Thank you for registering your email'
    conn = imaplib.IMAP4_SSL(server_name)
    conn.login(username, password)
    conn.select('Inbox')

    num_emails = 0
    sleeps = 6
    print("Waiting %ds for verification email..." % sleep)
    sleep = sleep / sleeps
    while num_emails == 0 and sleeps > 0:
        typ, data = conn.search(None, '(UNSEEN SUBJECT "%s")' % subject)
        unread_msg_nums = data[0].split()
        print("Sleeping %ds" % sleep)
        time.sleep(sleep)
        num_emails = len(unread_msg_nums)
        sleeps = sleeps - 1

    print('Verifying %d Account Email(s)' % num_emails)  # print the count of all unread messages

    for num in unread_msg_nums:  # data is a list. A space separated string
        typ, data = conn.fetch(num, '(RFC822)')  # fetch the email body (RFC822) for the given ID
        raw_email = data[0][1]  # here's the body, which is raw text of the whole email
        raw_email = str(raw_email)
        typ, data = conn.store(num, '-FLAGS', '\\Seen')  # marks email as read
        link = (re.search("(?P<url>http://echo7.bluehornet.com[^\s]+VALIDATE)", raw_email).group("url")).split("\"")[0]
        if link:
            response = s.get(link)
            if response:
                conn.store(num, '+FLAGS', '\Seen')  # marks email as read (again)
                print('EMAIL VERIFIED!')
            else:
                raise Exception('UNABLE TO OPEN LINK')
        else:
            raise Exception('UNABLE TO PARSE LINK FROM EMAIL')


class WaitForCaptcha():
    def __init__(self):
        self.waited_for = 0

    def sleep(self, seconds):
        self.waited_for += seconds

        for i in range(0, seconds):
            if i is seconds - 1 and self.waited_for % 10 is 0:
                if self.waited_for > 30:
                    print(". (%ds)" % self.waited_for)
                else:
                    print('.')
            else:
                print('.',
                      sys.stdout.flush())
            time.sleep(1)


def register_account(email, password, proxyIp=None, proxyUser=None, proxyPass=None, proxyPort=None):
    proxies = None
    print('''Registering account with:
    Email: %s
    Password: %s 
    Proxy: %s''' % (email, password, ('None' if proxyIp is None else proxyIp)))

    if proxyIp:
        if proxyUser and proxyPass and proxyUser != 'null' and proxyPass != 'null':
            proxies = {'http': 'socks5h://%s:%s@%s:%s' % (proxyUser, proxyPass, proxyIp, proxyPort),
                        'https': 'socks5h://%s:%s@%s:%s' % (proxyUser, proxyPass, proxyIp, proxyPort)}
        else:
            proxies = {'http': 'socks5h://%s:%s' % (proxyIp, proxyPort),
                        'https': 'socks5h://%s:%s' % (proxyIp, proxyPort)}

    s = requests.session()
    s.proxies = proxies

    data = {
        'theme': 'oldschool',
        'email1': email,
        'onlyOneEmail': 1,
        'password1': password,
        'onlyOnePassword': 1,
        'day': randint(1, 28),
        'month': randint(1, 12) < 10,
        'year': randint(1995, 2005),
        'agree_email': 1,
        'agree_email_third_party': 1,
        'g-recaptcha-response': solve_captcha(s),
        'create-submit': 'Play Now'
    }

    response = s.post(RUNESCAPE_REGISTER_URL, data=data)

    if response.status_code == requests.codes.ok:
        if 'Account Created' in response.text:
            print('Robots win again, account successfully registered\n\n')
            with open('C:\\Users\\Laure\\Documents\\RSPeer\\cache\\data\\AccountsGenerated.txt', 'a+') as f:
                if proxyIp and proxyUser and proxyPass and proxyUser != 'null' and proxyPass != 'null':
                    f.write('%s:%s:%s:%s:%s:%s\n' % (email, password, proxyIp, proxyUser, proxyPass, proxyPort))
                elif proxyIp:
                    f.write('%s:%s:%s:%s\n' % (email, password, proxyIp, proxyPort))
                else:
                    f.write('%s:%s\n' % (email, password))
                f.close()

            verify_email(s)
        else:
            print('JAGEX SAYS NO: Creation')
    else:
        print('JAGEX SAYS NO: Posting To Link')

    s.close()


def solve_captcha(s):
    print('Solving Captcha')
    waiting = True
    touched = False
    captcha_id = None

    params = {
        'key': CAPTCHA_API_KEY,
        'method': 'userrecaptcha',
        'googlekey': RUNESCAPE_RECAPTCHA_KEY,
        'pageurl': RUNESCAPE_REGISTER_URL
    }

    response = s.get(CAPTCHA_REQ_URL, params=params)

    if response.status_code != requests.codes.ok:
        raise Exception('2Captcha says no')

    if '|' in response.text:
        _, captcha_id = response.text.split('|')

    wait_for_captcha = WaitForCaptcha()

    print('Waiting for captcha (ID: %s) to be solved' % captcha_id)
    while waiting:
        wait_for_captcha.sleep(5 if touched else 15)

        touched = True

        params = {
            'key': CAPTCHA_API_KEY,
            'action': 'get',
            'id': captcha_id
        }

        solution_response = s.get(CAPTCHA_RES_URL, params=params)

        if solution_response.text not in ('CAPCHA_NOT_READY', 'CAPTCHA_NOT_READY'):
            print('\nCaptcha solved after %ds! (solution: %s)' % (wait_for_captcha.waited_for, solution_response.text))
            waiting = False
            _, captcha_solution = solution_response.text.split('|')
            return captcha_solution


def main():
    if not len(sys.argv) > 1:
        print('You forgot to pass in any arguments! Run with -h/--help for more info')
        sys.exit()

    parser = argparse.ArgumentParser(description='Create Runescape account(s)\n'
                                                 'Pass new account details or path to a file with list of them',
                                     formatter_class=argparse.RawTextHelpFormatter)

    acc_arg_group = parser.add_argument_group('Create an account')
    acc_arg_group.add_argument('-e2', '--email', nargs=1,
                               help='Email address to use for the new account')
    acc_arg_group.add_argument('-p2', '--password', nargs=1,
                               help='Password')

    proxy_acc_arg_group = parser.add_argument_group('Create an account with proxy')
    proxy_acc_arg_group.add_argument('-e', '--email_p', nargs=1,
                                     help='Email address to use for the new account')
    proxy_acc_arg_group.add_argument('-p', '--password_p', nargs=1,
                                     help='Password')
    proxy_acc_arg_group.add_argument('-i', '--proxyIp', nargs=1,
                                     help='Proxy ip')
    proxy_acc_arg_group.add_argument('-u', '--proxyUser', nargs=1,
                                     help='Proxy username')
    proxy_acc_arg_group.add_argument('-x', '--proxyPass', nargs=1,
                                     help='Proxy password')
    proxy_acc_arg_group.add_argument('-o', '--proxyPort', nargs=1,
                                     help='Proxy port')
    # acc_list_arg_group = parser.add_argument_group('Create accounts from a list')

    # acc_list_arg_group.add_argument('-l', '--list', nargs=1,
    #                                help='''Path to file with list of new account details
    #        Syntax within files should match:
    #        email:password''')

    args = parser.parse_args()

    # if args.list:
    #    accounts_file = open(args.list[0])
    #    accounts = accounts_file.readlines()
    #    accounts_file.close()
    #
    #    for account in accounts:
    #        email, password = account.rstrip().split(':')
    #        register_account(email, password)

    if args.email and args.password:
        register_account(args.email[0], args.password[0])

    elif args.email_p and args.password_p and args.proxyIp and args.proxyUser and args.proxyPass and args.proxyPort:
        register_account(args.email_p[0], args.password_p[0], args.proxyIp[0],
                         args.proxyUser[0], args.proxyPass[0], args.proxyPort[0])

    else:
        print('Not enough arguments! Run with -h/--help for more info')


if __name__ == '__main__':
    main()
