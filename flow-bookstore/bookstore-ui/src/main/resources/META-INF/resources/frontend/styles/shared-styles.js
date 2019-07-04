const $_documentContainer = document.createElement('template');

$_documentContainer.innerHTML = `<custom-style>
    <style>
        .login-information {
            min-width: 300px;
            flex: 0;
            background: var(--lumo-primary-color-50pct);
        }

        /* Stack login-screen vertically on narrow screen */
        @media (max-width: 800px) {

            .login-screen {
                flex-direction: column;
            }

            .login-information {
                display: block;
            }
        }

        .main-layout {
            flex-direction: row;

            /* Used by the menu and form overlays */
            --overlay-box-shadow: 0 0 3px 2px var(--lumo-contrast-10pct);
        }

        .menu-bar {
            flex-direction: column;
            flex-shrink: 0;
            background: var(--lumo-base-color);
            box-shadow: var(--overlay-box-shadow);
        }

        .menu-header {
            padding: 11px 16px;
        }

        .menu-bar vaadin-tabs {
            align-items: flex-start;
            transition: transform 300ms;
        }

        .menu-bar vaadin-tab {
            padding: 0;
        }

        .menu-link {
            margin: 0 auto;
            padding: 10px 37px;
        }

        .menu-link > span {
            display: block;
        }

        .menu-button {
            display: none;
        }

        .product-form {
            position: absolute;
            right: 0;
            bottom: 0;
            height: 100%;
            overflow: auto;
            background: var(--lumo-base-color);
            box-shadow: var(--overlay-box-shadow);
        }

        /* On narrow screens, move the side bar to the top,
         * except for the link-tabs, which are hidden but
         * can be opened via a button.
         */
        @media (max-width: 800px) {

            .main-layout {
                flex-direction: column;
                --top-bar-height: 50px;
                --top-bar-margin: 5vw;
            }

            .menu-bar {
                flex-direction: row;
                align-items: center;
                justify-content: space-between;
                height: var(--top-bar-height);
                margin: 0 var(--top-bar-margin);
                background: none;
                box-shadow: none;
            }

            .menu-bar vaadin-tabs {
                transform: translateX(calc(-100% - var(--top-bar-margin)));
                position: absolute;
                top: var(--top-bar-height);
                height: calc(100% - var(--top-bar-height));
                z-index: 100;
                box-shadow: var(--overlay-box-shadow);
                background: var(--lumo-base-color);
            }

            .menu-bar .show-tabs {
                transform: translateX(calc(0% - var(--top-bar-margin)));
            }

            .menu-button {
                display: block;
            }

            .product-form {
                height: calc(100% - var(--top-bar-height));
            }

        }

        @media (max-width: 550px) {

            .product-form {
                width: 100%;

                /* Prevent text-fields from overflowing on narrow screens */
                --vaadin-text-field-default-width: 6em;
            }
        }

        /* Color codes for the availability statuses of the products */
        .Available {
            color: #2dd085;
        }

        .Coming {
            color: #ffc66e;
        }

        .Discontinued {
            color: #f54993;
        }

    </style>
</custom-style>`;

document.head.appendChild($_documentContainer.content);
