def main():
    # code here
    pass

if __name__ == "__main__":
    main()

def _send_receipt_email(user: User, transaction: DbTransaction, email: str = None) -> bool:
    """Send an e-mail with the a receipt for the transaction to the user.
        Return true if email address found.
    """
    if not email:
        email = user.receipt_email
    if email:
        locale = user.locale
        if locale is None:
            locale = shared.config.default_locale
        logger.debug("Send receipt in %s to %s", locale, user.receipt_email)
        try:
            t = Template(name='receipt_email', locale=locale, global_variables=shared.jinja_globals)
            template_env = {'transaction': transaction}
            subject = t.render(template_env, section='subject')
            message = t.render(template_env, section='body')
            content_type = t.content_type
        except TemplateNotFound:
            subject = "Receipt from BobCat"
            message = f"Receipt for transaction: {transaction.id}"
            content_type = 'text/plain'
        services.email_sender.send(email_from=None, email_to=email, subject=subject, message=message,
                                   content_type=content_type)
        return True
    else:
        return False


def _remove_id_from_optional_set(id: str, id_set: Optional[Set[str]]):
    """Remove id from set. Return True if removed or no set."""
    if id_set is None:
        return True
    try:
        id_set.remove(id)
        return True
    except KeyError:
        return False


def _update_cancellable(transaction: DbTransaction) -> DbTransaction:
    """Update cancellable status"""
    if transaction.cancellable and transaction.cancellable_expire:
        if transaction.cancellable_expire < now():
            transaction.cancellable = False
            transaction.cancellable_expire = None
            transaction.save()
    return transaction
